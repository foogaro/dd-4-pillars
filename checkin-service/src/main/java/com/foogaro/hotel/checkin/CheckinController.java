package com.foogaro.hotel.checkin;

import com.foogaro.hotel.common.dto.CheckInRequest;
import com.foogaro.hotel.common.dto.CheckOutRequest;
import com.foogaro.hotel.common.model.Stay;
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

@RestController
@RequestMapping("/api/stays")
@Tag(name = "Stays", description = "Check-in and check-out")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public Stay checkIn(@Valid @RequestBody CheckInRequest request) {
        return checkinService.checkIn(request);
    }

    @PostMapping("/check-out")
    public Stay checkOut(@Valid @RequestBody CheckOutRequest request) {
        return checkinService.checkOut(request);
    }

    @GetMapping("/{id}")
    public Stay get(@PathVariable String id) {
        return checkinService.get(id);
    }

    @GetMapping("/by-booking/{bookingId}")
    public Stay getByBooking(@PathVariable String bookingId) {
        return checkinService.getByBooking(bookingId);
    }
}
