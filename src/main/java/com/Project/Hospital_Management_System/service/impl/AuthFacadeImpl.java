package com.hospital.doctor.service.impl;

import com.hospital.doctor.entity.Doctor;
import com.hospital.doctor.repository.DoctorRepository;
import com.hospital.doctor.service.AuthFacade;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthFacadeImpl implements AuthFacade {

    private final DoctorRepository doctorRepo;

    public AuthFacadeImpl(DoctorRepository doctorRepo) {
        this.doctorRepo = doctorRepo;
        System.out.println("AuthFacadeImpl wired. doctorRepo=" + (doctorRepo != null));
    }

    @Override
    public Long currentDoctorIdOrThrow() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName())) {
            throw new IllegalStateException("No authenticated user");
        }
        Doctor d = doctorRepo.findByUser_Username(a.getName())
                .orElseThrow(() -> new IllegalStateException("No doctor profile for current user"));
        return d.getId();
    }
}
