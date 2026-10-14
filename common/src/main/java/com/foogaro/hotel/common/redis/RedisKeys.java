package com.foogaro.hotel.common.redis;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String hotel(String id) {
        return "hotel:" + id;
    }

    public static String hotels() {
        return "hotels";
    }

    public static String room(String id) {
        return "room:" + id;
    }

    public static String hotelRooms(String hotelId) {
        return "hotel:" + hotelId + ":rooms";
    }

    public static String booking(String id) {
        return "booking:" + id;
    }

    public static String sessionBookings(String sessionId) {
        return "session:" + sessionId + ":bookings";
    }

    public static String stay(String id) {
        return "stay:" + id;
    }

    public static String bookingStay(String bookingId) {
        return "booking:" + bookingId + ":stay";
    }

    public static String workflow(String id) {
        return "workflow:" + id;
    }

    public static String sessionWorkflows(String sessionId) {
        return "session:" + sessionId + ":workflows";
    }

    public static String roomLock(String roomId, String checkIn, String checkOut) {
        return "lock:room:" + roomId + ":" + checkIn + ":" + checkOut;
    }
}
