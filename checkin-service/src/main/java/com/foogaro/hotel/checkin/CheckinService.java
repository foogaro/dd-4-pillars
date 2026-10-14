package com.foogaro.hotel.checkin;

import com.foogaro.hotel.common.dto.CheckInRequest;
import com.foogaro.hotel.common.dto.CheckOutRequest;
import com.foogaro.hotel.common.exception.BadRequestException;
import com.foogaro.hotel.common.exception.ConflictException;
import com.foogaro.hotel.common.model.Booking;
import com.foogaro.hotel.common.model.BookingStatus;
import com.foogaro.hotel.common.model.Stay;
import com.foogaro.hotel.common.model.StayStatus;
import com.foogaro.hotel.common.session.SessionContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class CheckinService {

    private static final Logger log = LoggerFactory.getLogger(CheckinService.class);

    private final StayRepository stayRepository;
    private final BookingClient bookingClient;
    private final Counter checkInCounter;
    private final Counter checkOutCounter;

    public CheckinService(StayRepository stayRepository, BookingClient bookingClient, MeterRegistry meterRegistry) {
        this.stayRepository = stayRepository;
        this.bookingClient = bookingClient;
        this.checkInCounter = meterRegistry.counter("stay.checkin");
        this.checkOutCounter = meterRegistry.counter("stay.checkout");
    }

    @Observed(name = "stay.checkin", contextualName = "stay-checkin")
    public Stay checkIn(CheckInRequest request) {
        Booking booking = bookingClient.getBooking(request.getBookingId());
        if (booking.getStatus() != BookingStatus.PAYMENT_ACCEPTED) {
            throw new BadRequestException("Check-in requires a booking with PAYMENT_ACCEPTED status, was "
                    + booking.getStatus());
        }
        stayRepository.findByBookingId(booking.getId()).ifPresent(existing -> {
            throw new ConflictException("Stay already exists for booking: " + booking.getId());
        });

        Stay stay = new Stay();
        stay.setId(UUID.randomUUID().toString());
        stay.setBookingId(booking.getId());
        stay.setSessionId(SessionContext.get() != null ? SessionContext.get() : booking.getSessionId());
        stay.setHotelId(booking.getHotelId());
        stay.setRoomId(booking.getRoomId());
        stay.setStatus(StayStatus.CHECKED_IN);
        stay.setCheckedInAt(Instant.now());
        Stay saved = stayRepository.save(stay);
        checkInCounter.increment();
        log.info("Checked in stayId={} bookingId={} roomId={}", saved.getId(), saved.getBookingId(), saved.getRoomId());
        return saved;
    }

    @Observed(name = "stay.checkout", contextualName = "stay-checkout")
    public Stay checkOut(CheckOutRequest request) {
        Stay stay = stayRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new BadRequestException("No check-in found for booking: " + request.getBookingId()));
        if (stay.getStatus() == StayStatus.CHECKED_OUT) {
            throw new ConflictException("Stay already checked out: " + stay.getId());
        }
        stay.setStatus(StayStatus.CHECKED_OUT);
        stay.setCheckedOutAt(Instant.now());
        Stay saved = stayRepository.save(stay);
        checkOutCounter.increment();
        log.info("Checked out stayId={} bookingId={}", saved.getId(), saved.getBookingId());
        return saved;
    }

    @Observed(name = "stay.get", contextualName = "stay-get")
    public Stay get(String id) {
        return stayRepository.getRequired(id);
    }

    @Observed(name = "stay.get-by-booking", contextualName = "stay-get-by-booking")
    public Stay getByBooking(String bookingId) {
        return stayRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BadRequestException("No stay found for booking: " + bookingId));
    }
}
