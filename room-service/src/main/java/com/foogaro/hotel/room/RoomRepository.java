package com.foogaro.hotel.room;

import com.foogaro.hotel.common.model.Room;
import com.foogaro.hotel.common.redis.JsonRedisStore;
import com.foogaro.hotel.common.redis.RedisKeys;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoomRepository {

    private final JsonRedisStore store;

    public RoomRepository(JsonRedisStore store) {
        this.store = store;
    }

    public Room save(Room room) {
        store.save(RedisKeys.room(room.getId()), room);
        store.addToSet(RedisKeys.hotelRooms(room.getHotelId()), room.getId());
        return room;
    }

    public Optional<Room> findById(String id) {
        return store.find(RedisKeys.room(id), Room.class);
    }

    public Room getRequired(String id) {
        return store.getRequired(RedisKeys.room(id), Room.class, "Room not found: " + id);
    }

    public List<Room> findByHotelId(String hotelId) {
        return store.findAll(RedisKeys.hotelRooms(hotelId), RedisKeys::room, Room.class);
    }

    public boolean delete(Room room) {
        store.removeFromSet(RedisKeys.hotelRooms(room.getHotelId()), room.getId());
        return store.delete(RedisKeys.room(room.getId()));
    }

    public boolean reserve(String roomId, String checkIn, String checkOut, String bookingRef) {
        String lockKey = RedisKeys.roomLock(roomId, checkIn, checkOut);
        return store.setIfAbsent(lockKey, bookingRef);
    }

    public boolean release(String roomId, String checkIn, String checkOut) {
        return store.deleteString(RedisKeys.roomLock(roomId, checkIn, checkOut));
    }

    public boolean isAvailable(String roomId, String checkIn, String checkOut) {
        return !store.stringExists(RedisKeys.roomLock(roomId, checkIn, checkOut));
    }
}
