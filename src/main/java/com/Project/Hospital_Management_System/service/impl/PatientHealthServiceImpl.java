package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.service.PatientHealthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PatientHealthServiceImpl implements PatientHealthService {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void saveHealthProfile(Long patientId, BigDecimal height, BigDecimal weightKg,
                                  String chronicConditions, String pastMedicalHistory,
                                  String currentMedications) {
        var q = em.createNativeQuery("CALL save_health_profile(?,?,?,?,?,?)");
        q.setParameter(1, patientId);
        q.setParameter(2, height);
        q.setParameter(3, weightKg);
        q.setParameter(4, chronicConditions);
        q.setParameter(5, pastMedicalHistory);
        q.setParameter(6, currentMedications);
        q.executeUpdate();
    }

    @Override
    @Transactional
    public void deleteHealthProfile(Long patientId) {
        var q = em.createNativeQuery("CALL delete_health_profile(?)");
        q.setParameter(1, patientId);
        q.executeUpdate();
    }
}
