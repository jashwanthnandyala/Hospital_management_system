package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.entity.MedicalRecord;
import com.Project.Hospital_Management_System.repository.AppointmentRepository;
import com.Project.Hospital_Management_System.repository.MedicalRecordRepository;
import com.Project.Hospital_Management_System.service.MedicalRecordService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository recordRepo;
    private final AppointmentRepository appointmentRepo;

    public MedicalRecordServiceImpl(MedicalRecordRepository recordRepo,
                                    AppointmentRepository appointmentRepo) {
        this.recordRepo = recordRepo;
        this.appointmentRepo = appointmentRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecord> getRecordsForDoctor(Long doctorId, Long patientId) {
        boolean related = appointmentRepo.existsByDoctorIdAndPatientId(doctorId, patientId);
        if (!related) {
            throw new AccessDeniedException("Unauthorized for this patient");
        }
        return recordRepo.findByDoctor_IdAndPatient_Id(doctorId, patientId);
    }
}
