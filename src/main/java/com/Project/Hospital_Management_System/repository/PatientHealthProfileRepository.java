package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.PatientHealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientHealthProfileRepository extends JpaRepository<PatientHealthProfile, Long> {
    Optional<PatientHealthProfile> findByPatient_Id(Long patientId);
}
