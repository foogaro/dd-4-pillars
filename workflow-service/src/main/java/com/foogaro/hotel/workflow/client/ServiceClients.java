package com.foogaro.hotel.workflow.client;

import com.foogaro.hotel.common.dto.AvailabilityRequest;
import com.foogaro.hotel.common.dto.AvailabilityResponse;
import com.foogaro.hotel.common.dto.BookingRequest;
import com.foogaro.hotel.common.model.Booking;
import com.foogaro.hotel.common.model.Hotel;
import com.foogaro.hotel.common.model.Room;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ServiceClients {

    private final RestClient hotelClient;
    private final RestClient roomClient;
    private final RestClient bookingClient;

    public ServiceClients(ObjectProvider<RestClient.Builder> builders,
                          @Value("${app.hotel-service-url}") String hotelUrl,
                          @Value("${app.room-service-url}") String roomUrl,
                          @Value("${app.booking-service-url}") String bookingUrl) {
        this.hotelClient = builders.getObject().baseUrl(hotelUrl).build();
        this.roomClient = builders.getObject().baseUrl(roomUrl).build();
        this.bookingClient = builders.getObject().baseUrl(bookingUrl).build();
    }

    public Hotel getHotel(String hotelId) {
        return hotelClient.get()
                .uri("/api/hotels/{id}", hotelId)
                .retrieve()
                .body(Hotel.class);
    }

    public Room getRoom(String roomId) {
        return roomClient.get()
                .uri("/api/rooms/{id}", roomId)
                .retrieve()
                .body(Room.class);
    }

    public AvailabilityResponse reserveRoom(AvailabilityRequest request, String bookingRef) {
        return roomClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/rooms/reserve")
                        .queryParam("bookingRef", bookingRef)
                        .build())
                .body(request)
                .retrieve()
                .body(AvailabilityResponse.class);
    }

    public void releaseRoom(AvailabilityRequest request) {
        roomClient.post()
                .uri("/api/rooms/release")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public Booking createBooking(BookingRequest request) {
        return bookingClient.post()
                .uri("/api/bookings")
                .body(request)
                .retrieve()
                .body(Booking.class);
    }

    public Booking cancelBooking(String bookingId) {
        return bookingClient.post()
                .uri("/api/bookings/{id}/cancel", bookingId)
                .retrieve()
                .body(Booking.class);
    }
}
