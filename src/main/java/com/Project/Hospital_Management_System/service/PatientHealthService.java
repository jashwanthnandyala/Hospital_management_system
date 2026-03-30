package com.Project.Hospital_Management_System.service;

import java.math.BigDecimal;

public interface PatientHealthService {
    void saveHealthProfile(Long patientId, BigDecimal height, BigDecimal weightKg,
                           String chronicConditions, String pastMedicalHistory,
                           String currentMedications);
    void deleteHealthProfile(Long patientId);
}
