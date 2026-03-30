package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.entity.MedicalRecord;
import com.Project.Hospital_Management_System.service.AuthFacade;
import com.Project.Hospital_Management_System.service.MedicalRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Doctor-scoped read API for patient's medical records.
 * Path: /api/doctors/medical-records/patient/{patientId}
 */
@RestController
@RequestMapping("/api/doctors/medical-records")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorMedicalRecordsQueryController {

    private final MedicalRecordService medicalRecordService;
    private final AuthFacade authFacade;

    public DoctorMedicalRecordsQueryController(MedicalRecordService medicalRecordService,
                                               AuthFacade authFacade) {
        this.medicalRecordService = medicalRecordService;
        this.authFacade = authFacade;
    }

    @GetMapping("/patient/{patientId}")
    public List<MedicalRecord> getPatientMedicalRecords(@PathVariable Long patientId) {
        Long doctorId = authFacade.currentDoctorIdOrThrow();
        return medicalRecordService.getRecordsForDoctor(doctorId, patientId);
    }
}