package com.foogaro.hotel.common.dto;

import com.foogaro.hotel.common.model.PaymentOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BookWorkflowRequest {

    @NotBlank
    private String hotelId;

    @NotBlank
    private String roomId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    private PaymentOutcome paymentOutcome;

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public PaymentOutcome getPaymentOutcome() {
        return paymentOutcome;
    }

    public void setPaymentOutcome(PaymentOutcome paymentOutcome) {
        this.paymentOutcome = paymentOutcome;
    }
}
