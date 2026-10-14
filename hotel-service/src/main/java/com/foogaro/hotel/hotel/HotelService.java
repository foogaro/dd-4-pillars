package com.foogaro.hotel.hotel;

import com.foogaro.hotel.common.dto.HotelRequest;
import com.foogaro.hotel.common.exception.NotFoundException;
import com.foogaro.hotel.common.model.Hotel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    private final HotelRepository repository;
    private final Counter createdCounter;
    private final Counter updatedCounter;
    private final Counter deletedCounter;

    public HotelService(HotelRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.createdCounter = meterRegistry.counter("hotel.created");
        this.updatedCounter = meterRegistry.counter("hotel.updated");
        this.deletedCounter = meterRegistry.counter("hotel.deleted");
    }

    @Observed(name = "hotel.create", contextualName = "hotel-create")
    public Hotel create(HotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setId(UUID.randomUUID().toString());
        hotel.setName(request.getName());
        hotel.setCity(request.getCity());
        hotel.setCountry(request.getCountry());
        hotel.setAddress(request.getAddress());
        hotel.setStars(request.getStars());
        Instant now = Instant.now();
        hotel.setCreatedAt(now);
        hotel.setUpdatedAt(now);
        Hotel saved = repository.save(hotel);
        createdCounter.increment();
        log.info("Created hotel id={} name={} city={}", saved.getId(), saved.getName(), saved.getCity());
        return saved;
    }

    @Observed(name = "hotel.get", contextualName = "hotel-get")
    public Hotel get(String id) {
        log.debug("Fetching hotel id={}", id);
        return repository.getRequired(id);
    }

    @Observed(name = "hotel.list", contextualName = "hotel-list")
    public List<Hotel> list() {
        List<Hotel> hotels = repository.findAll();
        log.info("Listed hotels count={}", hotels.size());
        return hotels;
    }

    @Observed(name = "hotel.update", contextualName = "hotel-update")
    public Hotel update(String id, HotelRequest request) {
        Hotel hotel = repository.getRequired(id);
        hotel.setName(request.getName());
        hotel.setCity(request.getCity());
        hotel.setCountry(request.getCountry());
        hotel.setAddress(request.getAddress());
        hotel.setStars(request.getStars());
        hotel.setUpdatedAt(Instant.now());
        Hotel saved = repository.save(hotel);
        updatedCounter.increment();
        log.info("Updated hotel id={}", saved.getId());
        return saved;
    }

    @Observed(name = "hotel.delete", contextualName = "hotel-delete")
    public void delete(String id) {
        if (!repository.delete(id)) {
            throw new NotFoundException("Hotel not found: " + id);
        }
        deletedCounter.increment();
        log.info("Deleted hotel id={}", id);
    }
}
