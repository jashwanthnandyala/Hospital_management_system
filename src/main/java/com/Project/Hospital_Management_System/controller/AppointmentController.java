package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.AppointmentResponse;
import com.Project.Hospital_Management_System.dto.CreateAppointmentRequest;
import com.Project.Hospital_Management_System.dto.UpdateStatusResponse;
import com.Project.Hospital_Management_System.service.AppointmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PATIENT')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Create a new appointment (validates doctor schedule, overlap, patient/doctor existence)
     */
    @PostMapping
    public AppointmentResponse create(@RequestBody CreateAppointmentRequest request) {
        return appointmentService.create(request);
    }

    /**
     * Search appointments with optional filters
     */
    @GetMapping
    public List<AppointmentResponse> search(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String status
    ) {
        return appointmentService.search(patientId, doctorId, status);
    }

    /**
     * Confirm a pending appointment
     */
    @PutMapping("/{id}/confirm")
    public UpdateStatusResponse confirm(@PathVariable Long id) {
        return appointmentService.confirm(id);
    }

    /**
     * Cancel an appointment
     */
    @PutMapping("/{id}/cancel")
    public UpdateStatusResponse cancel(@PathVariable Long id) {
        return appointmentService.cancel(id);
    }

    /**
     * Mark an appointment as completed
     */
    @PutMapping("/{id}/complete")
    public UpdateStatusResponse complete(@PathVariable Long id) {
        return appointmentService.complete(id);
    }
}