package com.foogaro.hotel.room;

import com.foogaro.hotel.common.dto.AvailabilityRequest;
import com.foogaro.hotel.common.dto.AvailabilityResponse;
import com.foogaro.hotel.common.dto.RoomRequest;
import com.foogaro.hotel.common.model.Room;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Room CRUD, availability checks, reserve and release")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room create(@Valid @RequestBody RoomRequest request) {
        return roomService.create(request);
    }

    @GetMapping
    public List<Room> list(@RequestParam String hotelId) {
        return roomService.list(hotelId);
    }

    @GetMapping("/{id}")
    public Room get(@PathVariable String id) {
        return roomService.get(id);
    }

    @PutMapping("/{id}")
    public Room update(@PathVariable String id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        roomService.delete(id);
    }

    @PostMapping("/availability")
    public AvailabilityResponse availability(@Valid @RequestBody AvailabilityRequest request) {
        return roomService.checkAvailability(request);
    }

    @PostMapping("/reserve")
    public AvailabilityResponse reserve(@Valid @RequestBody AvailabilityRequest request,
                                        @RequestParam(required = false) String bookingRef) {
        return roomService.reserve(request, bookingRef);
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@Valid @RequestBody AvailabilityRequest request) {
        roomService.release(request);
    }
}
