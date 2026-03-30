package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.service.PatientMedicalRecordService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Service
public class PatientMedicalRecordServiceImpl implements PatientMedicalRecordService {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public Long addRecord(Long patientId, Long appointmentId, String visitSummary,
                          String diagnosis, String doctorNotes) {
        var q = em.createNativeQuery("CALL add_medical_record(?,?,?,?,?)");
        q.setParameter(1, patientId);
        q.setParameter(2, appointmentId);
        q.setParameter(3, visitSummary);
        q.setParameter(4, diagnosis);
        q.setParameter(5, doctorNotes);
        Object obj = q.getSingleResult();
        return (obj instanceof BigInteger bi) ? bi.longValue() : Long.valueOf(String.valueOf(obj));
    }

    @Override
    @Transactional
    public void updateRecord(Long id, String visitSummary, String diagnosis, String doctorNotes) {
        var q = em.createNativeQuery("CALL update_medical_record(?,?,?,?)");
        q.setParameter(1, id);
        q.setParameter(2, visitSummary);
        q.setParameter(3, diagnosis);
        q.setParameter(4, doctorNotes);
        q.executeUpdate();
    }

    @Override
    @Transactional
    public void deleteRecord(Long patientId, Long recordId) {
        var q = em.createNativeQuery("CALL delete_medical_record(?,?)");
        q.setParameter(1, recordId);
        q.setParameter(2, patientId);
        q.executeUpdate();
    }
}
