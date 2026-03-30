package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.service.PatientService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;

@Service
public class PatientServiceImpl implements PatientService {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public Long createPatientProc(String fullName, LocalDate dob, String gender,
                                  String phone, String email, String bloodGroup) {
        var q = em.createNativeQuery("CALL create_patient(?,?,?,?,?,?)");
        q.setParameter(1, fullName);
        q.setParameter(2, dob);
        q.setParameter(3, gender);
        q.setParameter(4, phone);
        q.setParameter(5, email);
        q.setParameter(6, bloodGroup);
        Object obj = q.getSingleResult();
        return (obj instanceof BigInteger bi) ? bi.longValue() : Long.valueOf(String.valueOf(obj));
    }

    @Override
    @Transactional
    public void updatePatientProc(Long id, String fullName, LocalDate dob, String gender,
                                  String phone, String email, String bloodGroup, boolean active) {
        var q = em.createNativeQuery("CALL update_patient(?,?,?,?,?,?,?,?)");
        q.setParameter(1, id);
        q.setParameter(2, fullName);
        q.setParameter(3, dob);
        q.setParameter(4, gender);
        q.setParameter(5, phone);
        q.setParameter(6, email);
        q.setParameter(7, bloodGroup);
        q.setParameter(8, active ? 1 : 0);
        q.executeUpdate();
    }

    @Override
    @Transactional
    public void deletePatientProc(Long id) {
        var q = em.createNativeQuery("CALL delete_patient(?)");
        q.setParameter(1, id);
        q.executeUpdate();
    }
}
