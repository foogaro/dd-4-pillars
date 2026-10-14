package com.foogaro.hotel.common.model;

public enum WorkflowState {
    STARTED,
    ROOM_RESERVED,
    PAYMENT_IN_PROGRESS,
    PAYMENT_ACCEPTED,
    PAYMENT_DECLINED,
    CONFIRMED,
    COMPENSATING,
    FAILED,
    CANCEL_REQUESTED,
    ROOM_RELEASED,
    CANCELLED
}
