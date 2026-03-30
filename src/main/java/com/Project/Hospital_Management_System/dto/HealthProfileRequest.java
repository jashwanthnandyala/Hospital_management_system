package com.Project.Hospital_Management_System.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HealthProfileRequest {
    public BigDecimal height;
    public BigDecimal weightKg;
    public BigDecimal bmi;
    public String chronicConditions;
    public String pastMedicalHistory;
    public String currentMedications;
    public LocalDateTime lastUpdatedAt;
}
