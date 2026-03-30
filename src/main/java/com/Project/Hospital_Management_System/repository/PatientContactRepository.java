package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.PatientContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientContactRepository extends JpaRepository<PatientContact, Long> {
    List<PatientContact> findByPatient_Id(Long patientId);
    List<PatientContact> findByPatient_IdAndPrimaryTrue(Long patientId);
}
