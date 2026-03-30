package com.Project.Hospital_Management_System.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateAppointmentRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        @NotNull OffsetDateTime startTime,
        String reason
) {}
