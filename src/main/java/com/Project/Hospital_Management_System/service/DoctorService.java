package com.Project.Hospital_Management_System.service;

import com.Project.Hospital_Management_System.dto.DoctorRegistrationDto;

public interface DoctorService {

    Long registerDoctor(DoctorRegistrationDto dto);
}