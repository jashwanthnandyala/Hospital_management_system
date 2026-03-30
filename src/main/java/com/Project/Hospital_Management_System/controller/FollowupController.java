package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.CreateFollowupDto;
import com.Project.Hospital_Management_System.dto.UpdateFollowupDto;
import com.Project.Hospital_Management_System.entity.PatientFollowup;
import com.Project.Hospital_Management_System.repository.PatientFollowupRepository;
import com.Project.Hospital_Management_System.service.AuthFacade;
import com.Project.Hospital_Management_System.service.FollowupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Doctor follow-up endpoints.
 */
@RestController
@RequestMapping("/api/doctors/followups")
@PreAuthorize("hasRole('DOCTOR')")
public class FollowupController {

    private final FollowupService followupService;
    private final AuthFacade authFacade;
    private final PatientFollowupRepository followupRepository;

    public FollowupController(FollowupService followupService,
                              AuthFacade authFacade,
                              PatientFollowupRepository followupRepository) {
        this.followupService = followupService;
        this.authFacade = authFacade;
        this.followupRepository = followupRepository;
    }

    /**
     * Get followups for a patient. Returns all by default.
     * Use ?status=OPEN to filter only open ones.
     */
    @GetMapping("/patient/{patientId}")
    public List<PatientFollowup> getFollowupsForPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return followupRepository.findByPatient_IdAndStatusOrderByFollowupDateAsc(patientId, status.toUpperCase());
        }
        return followupRepository.findByPatient_IdOrderByFollowupDateAsc(patientId);
    }

    /**
     * Get a single followup by ID.
     */
    @GetMapping("/{followupId}")
    public ResponseEntity<PatientFollowup> getFollowupById(@PathVariable Long followupId) {
        return followupRepository.findById(followupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a follow-up for a patient (requires the doctor to have treated/seen the patient).
     */
    @PostMapping("/patient/{patientId}")
    public ResponseEntity<?> addFollowup(@PathVariable Long patientId,
                                         @Valid @RequestBody CreateFollowupDto dto) {
        Long doctorId = authFacade.currentDoctorIdOrThrow();
        followupService.addFollowup(
                doctorId,
                patientId,
                dto.appointmentId(),
                dto.followupDate(),
                dto.followupType(),
                dto.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Follow-up added successfully"));
    }

    /**
     * Update an existing follow-up's status and (optionally) new date.
     */
    @PatchMapping("/{followupId}")
    public ResponseEntity<?> updateFollowupStatus(@PathVariable Long followupId,
                                                  @Valid @RequestBody UpdateFollowupDto dto) {
        Long doctorId = authFacade.currentDoctorIdOrThrow();
        String status = dto.status();
        followupService.updateFollowupStatus(followupId, doctorId, status, dto.newDate());
        return ResponseEntity.ok(Map.of("message", "Follow-up updated successfully"));
    }

    /**
     * Delete a follow-up.
     */
    @DeleteMapping("/{followupId}")
    public ResponseEntity<?> deleteFollowup(@PathVariable Long followupId) {
        if (!followupRepository.existsById(followupId)) {
            return ResponseEntity.notFound().build();
        }
        followupRepository.deleteById(followupId);
        return ResponseEntity.ok(Map.of("message", "Follow-up deleted successfully"));
    }
}