package com.Project.Hospital_Management_System.service;

import com.Project.Hospital_Management_System.entity.MedicalRecord;

import java.util.List;

public interface MedicalRecordService {

    List<MedicalRecord> getRecordsForDoctor(Long doctorId, Long patientId);
}