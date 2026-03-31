package com.Project.Hospital_Management_System.service;

/**
 * Utility to retrieve the currently authenticated doctor's ID.
 */
public interface AuthFacade {
    Long currentDoctorIdOrThrow();
}
