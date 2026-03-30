package com.Project.Hospital_Management_System.dto;

import java.math.BigDecimal;

public record ProfessionalProfileDto(
        String education,
        String certifications,
        BigDecimal consultationFee,
        String languagesSpoken,
        String availabilityNotes
) {}
