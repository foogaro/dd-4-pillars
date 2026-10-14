package com.foogaro.hotel.common.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Workflow {

    private String id;
    private String sessionId;
    private String hotelId;
    private String roomId;
    private String bookingId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private PaymentOutcome paymentOutcome;
    private WorkflowState state;
    private String error;
    private List<WorkflowStep> history = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public Workflow() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

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

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
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

    public WorkflowState getState() {
        return state;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<WorkflowStep> getHistory() {
        return history;
    }

    public void setHistory(List<WorkflowStep> history) {
        this.history = history;
    }

    public void addStep(WorkflowStep step) {
        this.history.add(step);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
