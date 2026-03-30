package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByDoctor_IdAndPatient_Id(Long doctorId, Long patientId);
    List<MedicalRecord> findByPatient_Id(Long patientId);
    Optional<MedicalRecord> findByAppointment_Id(Long appointmentId);
}