package com.foogaro.hotel.booking;

import com.foogaro.hotel.common.model.Booking;
import com.foogaro.hotel.common.redis.JsonRedisStore;
import com.foogaro.hotel.common.redis.RedisKeys;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BookingRepository {

    private final JsonRedisStore store;

    public BookingRepository(JsonRedisStore store) {
        this.store = store;
    }

    public Booking save(Booking booking) {
        store.save(RedisKeys.booking(booking.getId()), booking);
        store.addToSet(RedisKeys.sessionBookings(booking.getSessionId()), booking.getId());
        return booking;
    }

    public Optional<Booking> findById(String id) {
        return store.find(RedisKeys.booking(id), Booking.class);
    }

    public Booking getRequired(String id) {
        return store.getRequired(RedisKeys.booking(id), Booking.class, "Booking not found: " + id);
    }

    public List<Booking> findBySession(String sessionId) {
        return store.findAll(RedisKeys.sessionBookings(sessionId), RedisKeys::booking, Booking.class);
    }
}
