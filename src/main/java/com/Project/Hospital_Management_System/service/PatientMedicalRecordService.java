package com.Project.Hospital_Management_System.service;

public interface PatientMedicalRecordService {
    Long addRecord(Long patientId, Long appointmentId, String visitSummary,
                   String diagnosis, String doctorNotes);
    void updateRecord(Long id, String visitSummary, String diagnosis, String doctorNotes);
    void deleteRecord(Long patientId, Long recordId);
}
