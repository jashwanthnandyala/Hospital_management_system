package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByExternalId(String externalId);
    List<Patient> findByFullNameContainingIgnoreCase(String name);
    Optional<Patient> findByPhone(String phone);
    Optional<Patient> findByEmail(String email);
}