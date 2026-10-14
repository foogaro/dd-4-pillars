package com.foogaro.hotel.common.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckInRequest {

    @NotBlank
    private String bookingId;

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
}
