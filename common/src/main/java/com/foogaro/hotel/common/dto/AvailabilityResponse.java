package com.foogaro.hotel.common.dto;

public class AvailabilityResponse {

    private boolean available;
    private String lockKey;
    private String message;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(boolean available, String lockKey, String message) {
        this.available = available;
        this.lockKey = lockKey;
        this.message = message;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
