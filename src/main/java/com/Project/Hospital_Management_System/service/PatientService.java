package com.Project.Hospital_Management_System.service;

import com.Project.Hospital_Management_System.entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface PatientService {
    Long createPatientProc(String fullName, LocalDate dob, String gender,
                           String phone, String email, String bloodGroup);
    void updatePatientProc(Long id, String fullName, LocalDate dob, String gender,
                           String phone, String email, String bloodGroup, boolean active);
    void deletePatientProc(Long id);

}