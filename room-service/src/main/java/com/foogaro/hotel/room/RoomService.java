package com.foogaro.hotel.room;

import com.foogaro.hotel.common.dto.AvailabilityRequest;
import com.foogaro.hotel.common.dto.AvailabilityResponse;
import com.foogaro.hotel.common.dto.RoomRequest;
import com.foogaro.hotel.common.exception.BadRequestException;
import com.foogaro.hotel.common.exception.ConflictException;
import com.foogaro.hotel.common.exception.NotFoundException;
import com.foogaro.hotel.common.model.Room;
import com.foogaro.hotel.common.redis.RedisKeys;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository repository;
    private final Counter createdCounter;
    private final Counter availabilityChecks;
    private final Counter reservations;
    private final Counter releases;

    public RoomService(RoomRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.createdCounter = meterRegistry.counter("room.created");
        this.availabilityChecks = meterRegistry.counter("room.availability.checks");
        this.reservations = meterRegistry.counter("room.reservations");
        this.releases = meterRegistry.counter("room.releases");
    }

    @Observed(name = "room.create", contextualName = "room-create")
    public Room create(RoomRequest request) {
        Room room = new Room();
        room.setId(UUID.randomUUID().toString());
        room.setHotelId(request.getHotelId());
        room.setNumber(request.getNumber());
        room.setType(request.getType());
        room.setCapacity(request.getCapacity());
        room.setPricePerNight(request.getPricePerNight());
        Instant now = Instant.now();
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        Room saved = repository.save(room);
        createdCounter.increment();
        log.info("Created room id={} hotelId={} number={}", saved.getId(), saved.getHotelId(), saved.getNumber());
        return saved;
    }

    @Observed(name = "room.get", contextualName = "room-get")
    public Room get(String id) {
        return repository.getRequired(id);
    }

    @Observed(name = "room.list", contextualName = "room-list")
    public List<Room> list(String hotelId) {
        if (hotelId == null || hotelId.isBlank()) {
            throw new BadRequestException("Query parameter hotelId is required");
        }
        List<Room> rooms = repository.findByHotelId(hotelId);
        log.info("Listed rooms hotelId={} count={}", hotelId, rooms.size());
        return rooms;
    }

    @Observed(name = "room.update", contextualName = "room-update")
    public Room update(String id, RoomRequest request) {
        Room room = repository.getRequired(id);
        if (!room.getHotelId().equals(request.getHotelId())) {
            repository.delete(room);
            room.setHotelId(request.getHotelId());
        }
        room.setNumber(request.getNumber());
        room.setType(request.getType());
        room.setCapacity(request.getCapacity());
        room.setPricePerNight(request.getPricePerNight());
        room.setUpdatedAt(Instant.now());
        Room saved = repository.save(room);
        log.info("Updated room id={}", saved.getId());
        return saved;
    }

    @Observed(name = "room.delete", contextualName = "room-delete")
    public void delete(String id) {
        Room room = repository.getRequired(id);
        if (!repository.delete(room)) {
            throw new NotFoundException("Room not found: " + id);
        }
        log.info("Deleted room id={}", id);
    }

    @Observed(name = "room.availability.check", contextualName = "room-availability-check")
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        validateDates(request);
        repository.getRequired(request.getRoomId());
        availabilityChecks.increment();
        boolean available = repository.isAvailable(
                request.getRoomId(),
                request.getCheckIn().toString(),
                request.getCheckOut().toString());
        String lockKey = RedisKeys.roomLock(
                request.getRoomId(),
                request.getCheckIn().toString(),
                request.getCheckOut().toString());
        log.info("Availability check roomId={} available={}", request.getRoomId(), available);
        return new AvailabilityResponse(available, lockKey, available ? "Room is available" : "Room is already reserved");
    }

    @Observed(name = "room.reserve", contextualName = "room-reserve")
    public AvailabilityResponse reserve(AvailabilityRequest request, String bookingRef) {
        validateDates(request);
        repository.getRequired(request.getRoomId());
        boolean reserved = repository.reserve(
                request.getRoomId(),
                request.getCheckIn().toString(),
                request.getCheckOut().toString(),
                bookingRef == null ? "reserved" : bookingRef);
        if (!reserved) {
            throw new ConflictException("Room is not available for the selected dates");
        }
        reservations.increment();
        String lockKey = RedisKeys.roomLock(
                request.getRoomId(),
                request.getCheckIn().toString(),
                request.getCheckOut().toString());
        log.info("Reserved room id={} lockKey={}", request.getRoomId(), lockKey);
        return new AvailabilityResponse(true, lockKey, "Room reserved");
    }

    @Observed(name = "room.release", contextualName = "room-release")
    public void release(AvailabilityRequest request) {
        validateDates(request);
        repository.release(
                request.getRoomId(),
                request.getCheckIn().toString(),
                request.getCheckOut().toString());
        releases.increment();
        log.info("Released room id={} checkIn={} checkOut={}",
                request.getRoomId(), request.getCheckIn(), request.getCheckOut());
    }

    private void validateDates(AvailabilityRequest request) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new BadRequestException("checkOut must be after checkIn");
        }
    }
}
