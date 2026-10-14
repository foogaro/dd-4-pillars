package com.foogaro.hotel.hotel;

import com.foogaro.hotel.common.dto.HotelRequest;
import com.foogaro.hotel.common.model.Hotel;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@Tag(name = "Hotels", description = "Create, read, update and delete hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Hotel create(@Valid @RequestBody HotelRequest request) {
        return hotelService.create(request);
    }

    @GetMapping
    public List<Hotel> list() {
        return hotelService.list();
    }

    @GetMapping("/{id}")
    public Hotel get(@PathVariable String id) {
        return hotelService.get(id);
    }

    @PutMapping("/{id}")
    public Hotel update(@PathVariable String id, @Valid @RequestBody HotelRequest request) {
        return hotelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        hotelService.delete(id);
    }
}
