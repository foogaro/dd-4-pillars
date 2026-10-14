package com.foogaro.hotel.checkin;

import com.foogaro.hotel.common.exception.NotFoundException;
import com.foogaro.hotel.common.model.Booking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class BookingClient {

    private final RestClient restClient;

    public BookingClient(RestClient.Builder builder,
                         @Value("${app.booking-service-url}") String bookingServiceUrl) {
        this.restClient = builder.baseUrl(bookingServiceUrl).build();
    }

    public Booking getBooking(String bookingId) {
        try {
            return restClient.get()
                    .uri("/api/bookings/{id}", bookingId)
                    .retrieve()
                    .body(Booking.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Booking not found: " + bookingId);
        }
    }
}
