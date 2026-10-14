package com.foogaro.hotel.workflow;

import com.foogaro.hotel.common.dto.AvailabilityRequest;
import com.foogaro.hotel.common.dto.BookWorkflowRequest;
import com.foogaro.hotel.common.dto.BookingRequest;
import com.foogaro.hotel.common.exception.BadRequestException;
import com.foogaro.hotel.common.exception.ConflictException;
import com.foogaro.hotel.common.model.Booking;
import com.foogaro.hotel.common.model.BookingStatus;
import com.foogaro.hotel.common.model.Hotel;
import com.foogaro.hotel.common.model.Room;
import com.foogaro.hotel.common.model.Workflow;
import com.foogaro.hotel.common.model.WorkflowState;
import com.foogaro.hotel.common.model.WorkflowStep;
import com.foogaro.hotel.common.session.SessionContext;
import com.foogaro.hotel.workflow.client.ServiceClients;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestrator.class);

    private final WorkflowRepository repository;
    private final ServiceClients clients;
    private final Counter bookStarted;
    private final Counter bookConfirmed;
    private final Counter bookFailed;
    private final Counter cancelCompleted;
    private final Counter failures;
    private final Timer stepTimer;

    public WorkflowOrchestrator(WorkflowRepository repository, ServiceClients clients, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.clients = clients;
        this.bookStarted = meterRegistry.counter("workflow.book.started");
        this.bookConfirmed = meterRegistry.counter("workflow.book.confirmed");
        this.bookFailed = meterRegistry.counter("workflow.book.failed");
        this.cancelCompleted = meterRegistry.counter("workflow.cancel.completed");
        this.failures = meterRegistry.counter("workflow.failures");
        this.stepTimer = meterRegistry.timer("workflow.step.duration");
    }

    @Observed(name = "workflow.book", contextualName = "workflow-book")
    public Workflow book(BookWorkflowRequest request) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new BadRequestException("checkOut must be after checkIn");
        }

        String sessionId = SessionContext.get();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setSessionId(sessionId);
        workflow.setHotelId(request.getHotelId());
        workflow.setRoomId(request.getRoomId());
        workflow.setCheckIn(request.getCheckIn());
        workflow.setCheckOut(request.getCheckOut());
        workflow.setPaymentOutcome(request.getPaymentOutcome());
        workflow.setState(WorkflowState.STARTED);
        Instant now = Instant.now();
        workflow.setCreatedAt(now);
        workflow.setUpdatedAt(now);
        addStep(workflow, "start", WorkflowState.STARTED, "ok", "Workflow started");
        repository.save(workflow);
        bookStarted.increment();
        MDC.put("workflow.id", workflow.getId());
        log.info("Booking workflow started id={} hotelId={} roomId={}",
                workflow.getId(), workflow.getHotelId(), workflow.getRoomId());

        try {
            validateHotelAndRoom(workflow);
            reserveRoom(workflow);
            Booking booking = createBookingAndPay(workflow);
            if (booking.getStatus() == BookingStatus.PAYMENT_ACCEPTED) {
                transition(workflow, WorkflowState.PAYMENT_ACCEPTED, "payment", "accepted",
                        "Payment accepted for booking " + booking.getId());
                transition(workflow, WorkflowState.CONFIRMED, "confirm", "ok", "Booking confirmed");
                bookConfirmed.increment();
                log.info("Booking workflow confirmed id={} bookingId={}", workflow.getId(), booking.getId());
            } else {
                transition(workflow, WorkflowState.PAYMENT_DECLINED, "payment", "declined",
                        "Payment declined for booking " + booking.getId());
                compensate(workflow);
                transition(workflow, WorkflowState.FAILED, "fail", "payment_declined",
                        "Workflow failed after payment decline");
                bookFailed.increment();
                log.warn("Booking workflow failed id={} bookingId={}", workflow.getId(), booking.getId());
            }
            return repository.save(workflow);
        } catch (RuntimeException ex) {
            failures.increment();
            bookFailed.increment();
            workflow.setError(ex.getMessage());
            addStep(workflow, "error", workflow.getState(), "error", ex.getMessage());
            tryCompensate(workflow);
            workflow.setState(WorkflowState.FAILED);
            workflow.setUpdatedAt(Instant.now());
            repository.save(workflow);
            log.error("Booking workflow failed id={} error={}", workflow.getId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("workflow.id");
        }
    }

    @Observed(name = "workflow.cancel", contextualName = "workflow-cancel")
    public Workflow cancel(String workflowId) {
        Workflow workflow = repository.getRequired(workflowId);
        MDC.put("workflow.id", workflow.getId());
        try {
            if (workflow.getState() != WorkflowState.CONFIRMED) {
                throw new ConflictException("Only CONFIRMED workflows can be cancelled, current=" + workflow.getState());
            }
            transition(workflow, WorkflowState.CANCEL_REQUESTED, "cancel-request", "ok", "Cancellation requested");

            stepTimer.record(() -> {
                clients.cancelBooking(workflow.getBookingId());
                addStep(workflow, "cancel-booking", WorkflowState.CANCEL_REQUESTED, "ok",
                        "Booking cancelled " + workflow.getBookingId());
            });

            AvailabilityRequest release = availability(workflow);
            stepTimer.record(() -> {
                clients.releaseRoom(release);
                transition(workflow, WorkflowState.ROOM_RELEASED, "release-room", "ok", "Room lock released");
            });

            transition(workflow, WorkflowState.CANCELLED, "cancel-complete", "ok", "Workflow cancelled");
            cancelCompleted.increment();
            log.info("Cancellation workflow completed id={} bookingId={}", workflow.getId(), workflow.getBookingId());
            return repository.save(workflow);
        } catch (RuntimeException ex) {
            failures.increment();
            workflow.setError(ex.getMessage());
            addStep(workflow, "cancel-error", workflow.getState(), "error", ex.getMessage());
            repository.save(workflow);
            log.error("Cancellation failed id={} error={}", workflow.getId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("workflow.id");
        }
    }

    @Observed(name = "workflow.get", contextualName = "workflow-get")
    public Workflow get(String id) {
        return repository.getRequired(id);
    }

    public List<Workflow> listForSession() {
        String sessionId = SessionContext.get();
        if (sessionId == null) {
            return List.of();
        }
        return repository.findBySession(sessionId);
    }

    private void validateHotelAndRoom(Workflow workflow) {
        stepTimer.record(() -> {
            try {
                Hotel hotel = clients.getHotel(workflow.getHotelId());
                Room room = clients.getRoom(workflow.getRoomId());
                if (!hotel.getId().equals(room.getHotelId())) {
                    throw new BadRequestException("Room does not belong to hotel");
                }
                addStep(workflow, "validate", WorkflowState.STARTED, "ok",
                        "Validated hotel=" + hotel.getName() + " room=" + room.getNumber());
                repository.save(workflow);
            } catch (RestClientResponseException ex) {
                throw new BadRequestException("Hotel/room validation failed: " + ex.getResponseBodyAsString());
            }
        });
    }

    private void reserveRoom(Workflow workflow) {
        stepTimer.record(() -> {
            AvailabilityRequest request = availability(workflow);
            clients.reserveRoom(request, workflow.getId());
            transition(workflow, WorkflowState.ROOM_RESERVED, "reserve-room", "ok", "Room reserved");
            repository.save(workflow);
        });
    }

    private Booking createBookingAndPay(Workflow workflow) {
        return stepTimer.record(() -> {
            Room room = clients.getRoom(workflow.getRoomId());
            long nights = ChronoUnit.DAYS.between(workflow.getCheckIn(), workflow.getCheckOut());
            BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(Math.max(nights, 1)));

            transition(workflow, WorkflowState.PAYMENT_IN_PROGRESS, "create-booking", "in_progress",
                    "Creating booking and processing payment");
            repository.save(workflow);

            BookingRequest bookingRequest = new BookingRequest();
            bookingRequest.setHotelId(workflow.getHotelId());
            bookingRequest.setRoomId(workflow.getRoomId());
            bookingRequest.setCheckIn(workflow.getCheckIn());
            bookingRequest.setCheckOut(workflow.getCheckOut());
            bookingRequest.setTotalPrice(total);
            bookingRequest.setPaymentOutcome(workflow.getPaymentOutcome());

            Booking booking = clients.createBooking(bookingRequest);
            workflow.setBookingId(booking.getId());
            workflow.setUpdatedAt(Instant.now());
            repository.save(workflow);
            return booking;
        });
    }

    private void compensate(Workflow workflow) {
        transition(workflow, WorkflowState.COMPENSATING, "compensate", "start", "Releasing reserved room");
        repository.save(workflow);
        clients.releaseRoom(availability(workflow));
        addStep(workflow, "compensate", WorkflowState.COMPENSATING, "ok", "Room lock released after decline");
    }

    private void tryCompensate(Workflow workflow) {
        if (workflow.getState() == WorkflowState.ROOM_RESERVED
                || workflow.getState() == WorkflowState.PAYMENT_IN_PROGRESS
                || workflow.getState() == WorkflowState.PAYMENT_DECLINED) {
            try {
                clients.releaseRoom(availability(workflow));
                addStep(workflow, "compensate", WorkflowState.COMPENSATING, "ok", "Compensated room lock");
            } catch (Exception ex) {
                log.warn("Compensation failed for workflow {}: {}", workflow.getId(), ex.getMessage());
            }
        }
    }

    private AvailabilityRequest availability(Workflow workflow) {
        AvailabilityRequest request = new AvailabilityRequest();
        request.setRoomId(workflow.getRoomId());
        request.setCheckIn(workflow.getCheckIn());
        request.setCheckOut(workflow.getCheckOut());
        return request;
    }

    private void transition(Workflow workflow, WorkflowState state, String name, String outcome, String detail) {
        workflow.setState(state);
        workflow.setUpdatedAt(Instant.now());
        addStep(workflow, name, state, outcome, detail);
    }

    private void addStep(Workflow workflow, String name, WorkflowState state, String outcome, String detail) {
        workflow.addStep(new WorkflowStep(name, state, outcome, detail));
    }
}
