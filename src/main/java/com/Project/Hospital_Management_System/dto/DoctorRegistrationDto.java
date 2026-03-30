package com.Project.Hospital_Management_System.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record DoctorRegistrationDto(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String fullName,
        String gender,
        LocalDate dob,
        @NotBlank String phone,
        @NotBlank String primarySpecialty,
        @NotBlank String registrationNumber,
        @NotBlank String registrationCouncil,
        @Min(0) Integer yearsExperience
) {}