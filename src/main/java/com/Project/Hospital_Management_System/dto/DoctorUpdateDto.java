package com.Project.Hospital_Management_System.dto;

public record DoctorUpdateDto(
        String fullName,
        String phone,
        String email,
        String primarySpecialty,
        Integer yearsExperience
) {}