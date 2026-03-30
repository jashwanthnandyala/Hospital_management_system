package com.Project.Hospital_Management_System.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateFollowupDto(
        Long appointmentId,
        @NotNull LocalDate followupDate,
        @NotBlank String followupType,
        String notes
) {}