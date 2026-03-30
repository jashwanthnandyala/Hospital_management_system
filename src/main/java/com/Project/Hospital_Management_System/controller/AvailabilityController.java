package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.AvailabilitySlotDto;
import com.Project.Hospital_Management_System.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService service;

    public AvailabilityController(AvailabilityService service) {
        this.service = service;
    }

    /**
     * Get available appointment slots for a doctor on a given date.
     */
    @GetMapping
    public List<AvailabilitySlotDto> getAvailability(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return service.getAvailability(doctorId, date, ZoneId.of("UTC"));
    }
}
