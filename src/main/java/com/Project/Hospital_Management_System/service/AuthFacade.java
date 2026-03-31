package com.hospital.doctor.service;

/**
 * Utility to retrieve the currently authenticated doctor's ID.
 */
public interface AuthFacade {
    Long currentDoctorIdOrThrow();
}
