package com.hospital.doctor.service;

import com.hospital.doctor.entity.MedicalRecord;

import java.util.List;

public interface MedicalRecordService {

    List<MedicalRecord> getRecordsForDoctor(Long doctorId, Long patientId);
}