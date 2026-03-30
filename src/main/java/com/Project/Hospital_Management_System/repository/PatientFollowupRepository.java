package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.PatientFollowup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientFollowupRepository extends JpaRepository<PatientFollowup, Long> {
    List<PatientFollowup> findByPatient_IdOrderByFollowupDateAsc(Long patientId);
    List<PatientFollowup> findByPatient_IdAndStatusOrderByFollowupDateAsc(Long patientId, String status);
}