package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.common.api.ApiResponse;
import com.Project.Hospital_Management_System.dto.PatientCreateRequest;
import com.Project.Hospital_Management_System.dto.PatientIdResponse;
import com.Project.Hospital_Management_System.dto.PatientUpdateRequest;
import com.Project.Hospital_Management_System.entity.Patient;
import com.Project.Hospital_Management_System.repository.PatientRepository;
import com.Project.Hospital_Management_System.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/patients")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
public class PatientController {

    private final PatientService patientService;
    private final PatientRepository patientRepository;

    public PatientController(PatientService patientService, PatientRepository patientRepository) {
        this.patientService = patientService;
        this.patientRepository = patientRepository;
    }

    /**
     * Create a patient via stored procedure
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PatientIdResponse>> create(
            @RequestBody PatientCreateRequest req, UriComponentsBuilder uriBuilder) {
        Long id = patientService.createPatientProc(
                req.fullName, req.dob, req.gender, req.phone, req.email, req.bloodGroup
        );
        var location = uriBuilder.path("/api/patients/{id}").buildAndExpand(id).toUri();
        var body = ApiResponse.ok("Patient created successfully.", new PatientIdResponse(id));
        return ResponseEntity.created(location).body(body);
    }

    /**
     * Get a single patient by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patient> get(@PathVariable Long id) {
        return patientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a patient via stored procedure
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id,
                                                     @RequestBody PatientUpdateRequest req) {
        patientService.updatePatientProc(
                id, req.fullName, req.dob, req.gender, req.phone, req.email, req.bloodGroup,
                req.active != null ? req.active : true
        );
        return ResponseEntity.ok(ApiResponse.ok("Patient updated successfully."));
    }

    /**
     * Soft-delete a patient via stored procedure
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        patientService.deletePatientProc(id);
        return ResponseEntity.ok(ApiResponse.ok("Patient deleted successfully."));
    }
}