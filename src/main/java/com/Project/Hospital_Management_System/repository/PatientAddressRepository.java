package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.PatientAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientAddressRepository extends JpaRepository<PatientAddress, Long> {
    List<PatientAddress> findByPatient_Id(Long patientId);
    List<PatientAddress> findByPatient_IdAndPrimaryTrue(Long patientId);
}
