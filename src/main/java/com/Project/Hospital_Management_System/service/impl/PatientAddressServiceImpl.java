package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.service.PatientAddressService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientAddressServiceImpl implements PatientAddressService {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void addAddress(Long patientId, String addressType, String line1, String line2,
                           String city, String state, String postal, String country, boolean primary) {
        var q = em.createNativeQuery("CALL add_patient_address(?,?,?,?,?,?,?,?,?)");
        q.setParameter(1, patientId);
        q.setParameter(2, addressType);
        q.setParameter(3, line1);
        q.setParameter(4, line2);
        q.setParameter(5, city);
        q.setParameter(6, state);
        q.setParameter(7, postal);
        q.setParameter(8, country);
        q.setParameter(9, primary ? 1 : 0);
        q.executeUpdate();
    }

    @Override
    @Transactional
    public void updateAddress(Long patientId, Long addressId, String addressType, String line1, String line2,
                              String city, String state, String postal, String country, boolean primary) {
        var q = em.createNativeQuery("CALL update_patient_address(?,?,?,?,?,?,?,?,?,?)");
        q.setParameter(1, addressId);
        q.setParameter(2, patientId);
        q.setParameter(3, addressType);
        q.setParameter(4, line1);
        q.setParameter(5, line2);
        q.setParameter(6, city);
        q.setParameter(7, state);
        q.setParameter(8, postal);
        q.setParameter(9, country);
        q.setParameter(10, primary ? 1 : 0);
        q.executeUpdate();
    }

    @Override
    @Transactional
    public void deleteAddress(Long patientId, Long addressId) {
        var q = em.createNativeQuery("CALL delete_patient_address(?,?)");
        q.setParameter(1, addressId);
        q.setParameter(2, patientId);
        q.executeUpdate();
    }
}
