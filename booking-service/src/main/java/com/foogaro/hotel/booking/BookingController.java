package com.foogaro.hotel.booking;

import com.foogaro.hotel.common.dto.BookingRequest;
import com.foogaro.hotel.common.model.Booking;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Create bookings with simulated payment accept/decline")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    @GetMapping
    public List<Booking> list() {
        return bookingService.listForSession();
    }

    @GetMapping("/{id}")
    public Booking get(@PathVariable String id) {
        return bookingService.get(id);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable String id) {
        return bookingService.cancel(id);
    }
}
