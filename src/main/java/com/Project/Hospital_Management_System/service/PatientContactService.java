package com.Project.Hospital_Management_System.service;

public interface PatientContactService {
    void addContact(Long patientId, String name, String relationship, String phone, String email, boolean isPrimary);
    void updateContact(Long patientId, Long contactId, String name, String relationship,
                       String phone, String email, boolean isPrimary);
    void deleteContact(Long patientId, Long contactId);
}
