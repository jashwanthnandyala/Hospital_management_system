package com.Project.Hospital_Management_System.dto;

import java.time.OffsetDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        Long doctorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status,
        String reason
) {}
