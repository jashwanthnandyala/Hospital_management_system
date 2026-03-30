package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.entity.PatientFollowup;
import com.Project.Hospital_Management_System.repository.PatientFollowupRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Patient-facing read-only followup endpoints.
 * Path: /api/patients/{patientId}/followups
 */
@RestController
@RequestMapping("/api/patients/{patientId}/followups")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class PatientFollowupController {

    private final PatientFollowupRepository followupRepository;

    public PatientFollowupController(PatientFollowupRepository followupRepository) {
        this.followupRepository = followupRepository;
    }

    /**
     * List all followups for a patient. Use ?status=OPEN to filter.
     */
    @GetMapping
    public List<PatientFollowup> list(@PathVariable Long patientId,
                                      @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return followupRepository.findByPatient_IdAndStatusOrderByFollowupDateAsc(patientId, status.toUpperCase());
        }
        return followupRepository.findByPatient_IdOrderByFollowupDateAsc(patientId);
    }

    /**
     * Get a single followup by ID (scoped to a patient).
     */
    @GetMapping("/{followupId}")
    public ResponseEntity<PatientFollowup> get(@PathVariable Long patientId,
                                               @PathVariable Long followupId) {
        return followupRepository.findById(followupId)
                .filter(f -> f.getPatient().getId().equals(patientId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
