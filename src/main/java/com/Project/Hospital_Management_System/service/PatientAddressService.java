package com.Project.Hospital_Management_System.service;

public interface PatientAddressService {
    void addAddress(Long patientId, String addressType, String line1, String line2,
                    String city, String state, String postal, String country, boolean primary);
    void updateAddress(Long patientId, Long addressId, String addressType, String line1, String line2,
                       String city, String state, String postal, String country, boolean primary);
    void deleteAddress(Long patientId, Long addressId);
}
