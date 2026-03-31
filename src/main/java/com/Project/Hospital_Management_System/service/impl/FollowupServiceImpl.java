package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.entity.*;
import com.Project.Hospital_Management_System.repository.AppointmentRepository;
import com.Project.Hospital_Management_System.repository.PatientFollowupRepository;
import com.Project.Hospital_Management_System.repository.PatientRepository;
import com.Project.Hospital_Management_System.service.FollowupService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FollowupServiceImpl implements FollowupService {

    private final AppointmentRepository apptRepo;
    private final PatientFollowupRepository followupRepo;
    private final PatientRepository patientRepo;

    public FollowupServiceImpl(AppointmentRepository apptRepo,
                               PatientFollowupRepository followupRepo,
                               PatientRepository patientRepo) {
        this.apptRepo = apptRepo;
        this.followupRepo = followupRepo;
        this.patientRepo = patientRepo;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('DOCTOR')")
    public void addFollowup(Long doctorId, Long patientId, Long appointmentId,
                            LocalDate date, String type, String notes) {
        if (!apptRepo.existsByDoctorIdAndPatientId(doctorId, patientId)) {
            throw new SecurityException("Doctor not authorized for this patient");
        }

        PatientFollowup followup = new PatientFollowup();
        followup.setPatient(new Patient(patientId));
        followup.setDoctor(new Doctor(doctorId));
        if (appointmentId != null) {
            followup.setAppointment(new Appointment(appointmentId));
        }
        followup.setFollowupDate(date);
        followup.setFollowupType(type.toUpperCase());
        followup.setNotes(notes);
        followup.setStatus(PatientFollowup.STATUS_OPEN);
        followupRepo.save(followup);

        // Update patient's next followup snapshot
        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        patient.setNextFollowupDate(date);
        patient.setNextFollowupFromDoctorId(doctorId);
        patientRepo.save(patient);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('DOCTOR')")
    public void updateFollowupStatus(Long followupId, Long doctorId,
                                     String status, LocalDate newDate) {
        PatientFollowup followup = followupRepo.findById(followupId)
                .orElseThrow(() -> new IllegalArgumentException("Follow-up not found"));

        if (!followup.getDoctor().getId().equals(doctorId)) {
            throw new SecurityException("Not authorized to update this follow-up");
        }

        followup.setStatus(status);
        if (newDate != null) {
            followup.setFollowupDate(newDate);
        }
        followupRepo.save(followup);

        // Recompute patient's next followup snapshot (earliest OPEN)
        Long patientId = followup.getPatient().getId();
        List<PatientFollowup> openFollowups = followupRepo
                .findByPatient_IdAndStatusOrderByFollowupDateAsc(patientId, PatientFollowup.STATUS_OPEN);

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        if (openFollowups.isEmpty()) {
            patient.setNextFollowupDate(null);
            patient.setNextFollowupFromDoctorId(null);
        } else {
            PatientFollowup earliest = openFollowups.get(0);
            patient.setNextFollowupDate(earliest.getFollowupDate());
            patient.setNextFollowupFromDoctorId(earliest.getDoctor().getId());
        }
        patientRepo.save(patient);
    }
}
