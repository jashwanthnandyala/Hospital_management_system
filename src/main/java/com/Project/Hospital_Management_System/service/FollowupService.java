package com.Project.Hospital_Management_System.service;

import java.time.LocalDate;

public interface FollowupService {

    void addFollowup(Long doctorId, Long patientId, Long appointmentId,
                     LocalDate date, String type, String notes);

    void updateFollowupStatus(Long followupId, Long doctorId,
                              String status, LocalDate newDate);
}