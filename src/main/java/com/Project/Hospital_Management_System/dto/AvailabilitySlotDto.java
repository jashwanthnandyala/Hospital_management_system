package com.Project.Hospital_Management_System.dto;

import java.time.OffsetDateTime;

public record AvailabilitySlotDto(
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {}
