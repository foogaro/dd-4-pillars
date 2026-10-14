package com.foogaro.hotel.checkin;

import com.foogaro.hotel.common.model.Stay;
import com.foogaro.hotel.common.redis.JsonRedisStore;
import com.foogaro.hotel.common.redis.RedisKeys;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class StayRepository {

    private final JsonRedisStore store;

    public StayRepository(JsonRedisStore store) {
        this.store = store;
    }

    public Stay save(Stay stay) {
        store.save(RedisKeys.stay(stay.getId()), stay);
        store.setString(RedisKeys.bookingStay(stay.getBookingId()), stay.getId());
        return stay;
    }

    public Optional<Stay> findById(String id) {
        return store.find(RedisKeys.stay(id), Stay.class);
    }

    public Optional<Stay> findByBookingId(String bookingId) {
        return store.getString(RedisKeys.bookingStay(bookingId)).flatMap(this::findById);
    }

    public Stay getRequired(String id) {
        return store.getRequired(RedisKeys.stay(id), Stay.class, "Stay not found: " + id);
    }

    public boolean delete(String id) {
        Optional<Stay> stay = findById(id);
        stay.ifPresent(s -> store.deleteString(RedisKeys.bookingStay(s.getBookingId())));
        return store.delete(RedisKeys.stay(id));
    }
}
