package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.common.api.ApiResponse;
import com.Project.Hospital_Management_System.dto.HealthProfileRequest;
import com.Project.Hospital_Management_System.repository.PatientHealthProfileRepository;
import com.Project.Hospital_Management_System.service.PatientHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients/{patientId}/health")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class HealthController {

    private final PatientHealthService healthService;
    private final PatientHealthProfileRepository healthRepository;

    public HealthController(PatientHealthService healthService,
                            PatientHealthProfileRepository healthRepository) {
        this.healthService = healthService;
        this.healthRepository = healthRepository;
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> save(@PathVariable Long patientId,
                                                   @RequestBody HealthProfileRequest req) {
        healthService.saveHealthProfile(
                patientId, req.height, req.weightKg, req.chronicConditions,
                req.pastMedicalHistory, req.currentMedications
        );
        return ResponseEntity.ok(ApiResponse.ok("Health profile saved successfully."));
    }

    @GetMapping
    public ResponseEntity<HealthProfileRequest> get(@PathVariable Long patientId) {
        return healthRepository.findByPatient_Id(patientId)
                .map(h -> {
                    HealthProfileRequest r = new HealthProfileRequest();
                    r.height = h.getHeight();
                    r.weightKg = h.getWeightKg();
                    r.bmi = h.getBmi();
                    r.chronicConditions = h.getChronicConditions();
                    r.pastMedicalHistory = h.getPastMedicalHistory();
                    r.currentMedications = h.getCurrentMedications();
                    r.lastUpdatedAt = h.getLastUpdatedAt();
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long patientId) {
        healthService.deleteHealthProfile(patientId);
        return ResponseEntity.ok(ApiResponse.ok("Health profile deleted successfully."));
    }
}
