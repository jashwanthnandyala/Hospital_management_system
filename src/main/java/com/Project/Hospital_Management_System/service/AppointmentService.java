package com.Project.Hospital_Management_System.service;

import com.Project.Hospital_Management_System.dto.AppointmentResponse;
import com.Project.Hospital_Management_System.dto.CreateAppointmentRequest;
import com.Project.Hospital_Management_System.dto.UpdateStatusResponse;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse create(CreateAppointmentRequest request);

    List<AppointmentResponse> search(Long patientId, Long doctorId, String status);

    UpdateStatusResponse confirm(Long id);

    UpdateStatusResponse cancel(Long id);

    UpdateStatusResponse complete(Long id);
}