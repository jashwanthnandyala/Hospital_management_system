package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.common.api.ApiResponse;
import com.Project.Hospital_Management_System.dto.MedicalRecordCreateRequest;
import com.Project.Hospital_Management_System.dto.MedicalRecordUpdateRequest;
import com.Project.Hospital_Management_System.entity.MedicalRecord;
import com.Project.Hospital_Management_System.repository.MedicalRecordRepository;
import com.Project.Hospital_Management_System.service.PatientMedicalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Patient-scoped medical record endpoints following the patient service pattern.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
public class MedicalRecordController {

    private final PatientMedicalRecordService recordService;
    private final MedicalRecordRepository recordRepository;

    public MedicalRecordController(PatientMedicalRecordService recordService,
                                   MedicalRecordRepository recordRepository) {
        this.recordService = recordService;
        this.recordRepository = recordRepository;
    }

    /**
     * Create a medical record for a patient
     */
    @PostMapping("/patients/{patientId}/records")
    public ResponseEntity<ApiResponse<Long>> add(@PathVariable Long patientId,
                                                  @RequestBody MedicalRecordCreateRequest req,
                                                  UriComponentsBuilder uriBuilder) {
        Long id = recordService.addRecord(patientId, req.appointmentId, req.visitSummary, req.diagnosis, req.doctorNotes);
        var location = uriBuilder.path("/api/records/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.ok("Medical record created successfully.", id));
    }

    /**
     * Update a medical record
     */
    @PutMapping("/records/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id,
                                                     @RequestBody MedicalRecordUpdateRequest req) {
        recordService.updateRecord(id, req.visitSummary, req.diagnosis, req.doctorNotes);
        return ResponseEntity.ok(ApiResponse.ok("Medical record updated successfully."));
    }

    /**
     * Get a medical record by ID
     */
    @GetMapping("/records/{id}")
    public ResponseEntity<MedicalRecord> get(@PathVariable Long id) {
        return recordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all medical records for a patient
     */
    @GetMapping("/patients/{patientId}/records")
    public List<MedicalRecord> listByPatient(@PathVariable Long patientId) {
        return recordRepository.findByPatient_Id(patientId);
    }

    /**
     * Look up a medical record by appointment ID
     */
    @GetMapping("/records/by-appointment/{appointmentId}")
    public ResponseEntity<MedicalRecord> byAppointment(@PathVariable Long appointmentId) {
        return recordRepository.findByAppointment_Id(appointmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a medical record
     */
    @DeleteMapping("/patients/{patientId}/records/{recordId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long patientId,
                                                     @PathVariable Long recordId) {
        recordService.deleteRecord(patientId, recordId);
        return ResponseEntity.ok(ApiResponse.ok("Medical record deleted successfully."));
    }
}