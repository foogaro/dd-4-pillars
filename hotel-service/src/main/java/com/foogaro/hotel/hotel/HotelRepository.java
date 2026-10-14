package com.foogaro.hotel.hotel;

import com.foogaro.hotel.common.model.Hotel;
import com.foogaro.hotel.common.redis.JsonRedisStore;
import com.foogaro.hotel.common.redis.RedisKeys;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HotelRepository {

    private final JsonRedisStore store;

    public HotelRepository(JsonRedisStore store) {
        this.store = store;
    }

    public Hotel save(Hotel hotel) {
        store.save(RedisKeys.hotel(hotel.getId()), hotel);
        store.addToSet(RedisKeys.hotels(), hotel.getId());
        return hotel;
    }

    public Optional<Hotel> findById(String id) {
        return store.find(RedisKeys.hotel(id), Hotel.class);
    }

    public Hotel getRequired(String id) {
        return store.getRequired(RedisKeys.hotel(id), Hotel.class, "Hotel not found: " + id);
    }

    public List<Hotel> findAll() {
        return store.findAll(RedisKeys.hotels(), RedisKeys::hotel, Hotel.class);
    }

    public boolean delete(String id) {
        store.removeFromSet(RedisKeys.hotels(), id);
        return store.delete(RedisKeys.hotel(id));
    }
}
