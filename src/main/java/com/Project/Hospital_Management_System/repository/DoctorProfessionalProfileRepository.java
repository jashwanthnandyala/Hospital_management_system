package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.DoctorProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorProfessionalProfileRepository extends JpaRepository<DoctorProfessionalProfile, Long> {
    Optional<DoctorProfessionalProfile> findByDoctorId(Long doctorId);
}
