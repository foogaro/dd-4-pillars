package com.foogaro.hotel.booking;

import com.foogaro.hotel.common.dto.BookingRequest;
import com.foogaro.hotel.common.exception.BadRequestException;
import com.foogaro.hotel.common.exception.ConflictException;
import com.foogaro.hotel.common.model.Booking;
import com.foogaro.hotel.common.model.BookingStatus;
import com.foogaro.hotel.common.model.PaymentOutcome;
import com.foogaro.hotel.common.session.SessionContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository repository;
    private final Counter paymentInProgress;
    private final Counter paymentAccepted;
    private final Counter paymentDeclined;
    private final Counter cancelled;
    private final Timer paymentLatency;

    public BookingService(BookingRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.paymentInProgress = meterRegistry.counter("booking.payment.in_progress");
        this.paymentAccepted = meterRegistry.counter("booking.payment.accepted");
        this.paymentDeclined = meterRegistry.counter("booking.payment.declined");
        this.cancelled = meterRegistry.counter("booking.cancelled");
        this.paymentLatency = meterRegistry.timer("booking.payment.latency");
    }

    @Observed(name = "booking.create", contextualName = "booking-create")
    public Booking create(BookingRequest request) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new BadRequestException("checkOut must be after checkIn");
        }
        String sessionId = SessionContext.get();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID().toString());
        booking.setSessionId(sessionId);
        booking.setHotelId(request.getHotelId());
        booking.setRoomId(request.getRoomId());
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setTotalPrice(request.getTotalPrice());
        booking.setStatus(BookingStatus.PAYMENT_IN_PROGRESS);
        booking.setPaymentOutcome(request.getPaymentOutcome());
        Instant now = Instant.now();
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        repository.save(booking);
        paymentInProgress.increment();
        log.info("Booking created id={} status=PAYMENT_IN_PROGRESS roomId={}", booking.getId(), booking.getRoomId());

        return processPayment(booking);
    }

    @Observed(name = "booking.payment", contextualName = "booking-payment")
    Booking processPayment(Booking booking) {
        return paymentLatency.record(() -> {
            simulatePaymentDelay();
            PaymentOutcome outcome = booking.getPaymentOutcome();
            if (outcome == null) {
                outcome = ThreadLocalRandom.current().nextBoolean() ? PaymentOutcome.ACCEPT : PaymentOutcome.DECLINE;
                booking.setPaymentOutcome(outcome);
            }
            if (outcome == PaymentOutcome.ACCEPT) {
                booking.setStatus(BookingStatus.PAYMENT_ACCEPTED);
                paymentAccepted.increment();
                log.info("Payment accepted bookingId={} amount={}", booking.getId(), booking.getTotalPrice());
            } else {
                booking.setStatus(BookingStatus.PAYMENT_DECLINED);
                paymentDeclined.increment();
                log.warn("Payment declined bookingId={} amount={}", booking.getId(), booking.getTotalPrice());
            }
            booking.setUpdatedAt(Instant.now());
            return repository.save(booking);
        });
    }

    @Observed(name = "booking.get", contextualName = "booking-get")
    public Booking get(String id) {
        return repository.getRequired(id);
    }

    @Observed(name = "booking.list", contextualName = "booking-list")
    public List<Booking> listForSession() {
        String sessionId = SessionContext.get();
        if (sessionId == null) {
            return List.of();
        }
        return repository.findBySession(sessionId);
    }

    @Observed(name = "booking.cancel", contextualName = "booking-cancel")
    public Booking cancel(String id) {
        Booking booking = repository.getRequired(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }
        if (booking.getStatus() == BookingStatus.PAYMENT_DECLINED) {
            throw new ConflictException("Cannot cancel a declined booking");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(Instant.now());
        cancelled.increment();
        log.info("Booking cancelled id={}", id);
        return repository.save(booking);
    }

    private void simulatePaymentDelay() {
        try {
            Thread.sleep(Duration.ofMillis(50 + new Random().nextInt(150)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
