package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.service.PatientContactService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientContactServiceImpl implements PatientContactService {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void addContact(Long patientId, String name, String relationship, String phone, String email, boolean isPrimary) {
        var q = em.createNativeQuery("CALL add_patient_contact(?,?,?,?,?,?)");
        q.setParameter(1, patientId);
        q.setParameter(2, name);
        q.setParameter(3, relationship);
        q.setParameter(4, phone);
        q.setParameter(5, email);
        q.setParameter(6, isPrimary ? 1 : 0);
        q.executeUpdate();
    }

    @Override
    @Transactional
    public void updateContact(Long patientId, Long contactId, String name, String relationship,
                              String phone, String email, boolean isPrimary) {
        var q = em.createNativeQuery("CALL update_patient_contact(?,?,?,?,?,?,?)");
        q.setParameter(1, contactId);
        q.setParameter(2, patientId);
        q.setParameter(3, name);
        q.setParameter(4, relationship);
        q.setParameter(5, phone);
        q.setParameter(6, email);
        q.setParameter(7, isPrimary ? 1 : 0);
        q.executeUpdate();
    }

    @Override
    @Transactional
    public void deleteContact(Long patientId, Long contactId) {
        var q = em.createNativeQuery("CALL delete_patient_contact(?,?)");
        q.setParameter(1, contactId);
        q.setParameter(2, patientId);
        q.executeUpdate();
    }
}
