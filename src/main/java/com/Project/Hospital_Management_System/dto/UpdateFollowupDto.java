package com.Project.Hospital_Management_System.dto;

import java.time.LocalDate;

public record UpdateFollowupDto(
        String status,
        LocalDate newDate
) {}