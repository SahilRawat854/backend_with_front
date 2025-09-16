package com.spingo.bikerental.controller;

import com.spingo.bikerental.Bike;
import com.spingo.bikerental.BikeStatus;
import com.spingo.bikerental.BikeType;
import com.spingo.bikerental.BikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bikes")
@CrossOrigin(origins = "*")
public class BikeController {

    @Autowired
    private BikeRepository bikeRepository;

    // Get all bikes (public access)
    @GetMapping
    public ResponseEntity<List<Bike>> getAllBikes() {
        List<Bike> bikes = bikeRepository.findByIsActiveTrue();
        return ResponseEntity.ok(bikes);
    }

    // Get available bikes (public access)
    @GetMapping("/available")
    public ResponseEntity<List<Bike>> getAvailableBikes() {
        List<Bike> bikes = bikeRepository.findByIsActiveTrueAndStatus(BikeStatus.AVAILABLE);
        return ResponseEntity.ok(bikes);
    }

    // Get bike by ID (public access)
    @GetMapping("/{id}")
    public ResponseEntity<Bike> getBikeById(@PathVariable Long id) {
        Optional<Bike> bike = bikeRepository.findById(id);
        return bike.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    // Create new bike (Admin, Individual Owner, Rental Business only)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INDIVIDUAL_OWNER', 'RENTAL_BUSINESS')")
    public ResponseEntity<Bike> createBike(@Valid @RequestBody Bike bike) {
        Bike savedBike = bikeRepository.save(bike);
        return ResponseEntity.ok(savedBike);
    }

    // Update bike (Admin, Individual Owner, Rental Business only)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INDIVIDUAL_OWNER', 'RENTAL_BUSINESS')")
    public ResponseEntity<Bike> updateBike(@PathVariable Long id, @Valid @RequestBody Bike bikeDetails) {
        Optional<Bike> bikeOptional = bikeRepository.findById(id);
        if (bikeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Bike bike = bikeOptional.get();
        bike.setBrand(bikeDetails.getBrand());
        bike.setModel(bikeDetails.getModel());
        bike.setYear(bikeDetails.getYear());
        bike.setType(bikeDetails.getType());
        bike.setCity(bikeDetails.getCity());
        bike.setPricePerHour(bikeDetails.getPricePerHour());
        bike.setDescription(bikeDetails.getDescription());
        bike.setImageUrl(bikeDetails.getImageUrl());
        bike.setStatus(bikeDetails.getStatus());

        Bike updatedBike = bikeRepository.save(bike);
        return ResponseEntity.ok(updatedBike);
    }

    // Delete bike (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBike(@PathVariable Long id) {
        if (!bikeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        bikeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Get bikes by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Bike>> getBikesByStatus(@PathVariable BikeStatus status) {
        List<Bike> bikes = bikeRepository.findByStatus(status);
        return ResponseEntity.ok(bikes);
    }

    // Get bikes by type
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Bike>> getBikesByType(@PathVariable BikeType type) {
        List<Bike> bikes = bikeRepository.findByType(type);
        return ResponseEntity.ok(bikes);
    }

    // Get bikes by city
    @GetMapping("/city/{city}")
    public ResponseEntity<List<Bike>> getBikesByCity(@PathVariable String city) {
        List<Bike> bikes = bikeRepository.findByCity(city);
        return ResponseEntity.ok(bikes);
    }

    // Get bikes by brand
    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<Bike>> getBikesByBrand(@PathVariable String brand) {
        List<Bike> bikes = bikeRepository.findByBrand(brand);
        return ResponseEntity.ok(bikes);
    }

    // Filter bikes with multiple criteria
    @GetMapping("/filter")
    public ResponseEntity<List<Bike>> filterBikes(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BikeType type,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BikeStatus status) {
        
        List<Bike> bikes = bikeRepository.findBikesWithFilters(city, type, brand, status);
        return ResponseEntity.ok(bikes);
    }

    // Get popular bikes (public access)
    @GetMapping("/popular")
    public ResponseEntity<List<Bike>> getPopularBikes() {
        // Get first 3 available bikes as popular bikes
        List<Bike> popularBikes = bikeRepository.findByIsActiveTrueAndStatus(BikeStatus.AVAILABLE)
                .stream()
                .limit(3)
                .toList();
        return ResponseEntity.ok(popularBikes);
    }

    // Check bike availability for specific time slot
    @GetMapping("/{id}/availability")
    public ResponseEntity<Map<String, Object>> checkBikeAvailability(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        Optional<Bike> bikeOptional = bikeRepository.findById(id);
        if (bikeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Bike bike = bikeOptional.get();
        Map<String, Object> response = new HashMap<>();
        
        // Check if bike is available
        if (bike.getStatus() != BikeStatus.AVAILABLE) {
            response.put("available", false);
            response.put("reason", "Bike is currently " + bike.getStatus().name().toLowerCase());
            return ResponseEntity.ok(response);
        }
        
        // TODO: Add time slot conflict checking with booking repository
        response.put("available", true);
        response.put("bike", bike);
        
        return ResponseEntity.ok(response);
    }
}