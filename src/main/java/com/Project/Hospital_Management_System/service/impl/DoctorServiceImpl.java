package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.dto.DoctorRegistrationDto;
import com.Project.Hospital_Management_System.entity.*;
import com.Project.Hospital_Management_System.repository.*;
import com.Project.Hospital_Management_System.service.DoctorService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final DoctorRepository doctorRepo;
    private final UserRoleRepository userRoleRepo;
    private final PasswordEncoder encoder;

    public DoctorServiceImpl(UserRepository userRepo,
                             RoleRepository roleRepo,
                             DoctorRepository doctorRepo,
                             UserRoleRepository userRoleRepo,
                             PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.doctorRepo = doctorRepo;
        this.userRoleRepo = userRoleRepo;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public Long registerDoctor(DoctorRegistrationDto dto) {
        // user
        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPasswordHash(encoder.encode(dto.password()));
        user = userRepo.save(user);

        // doctor
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setFullName(dto.fullName());
        doctor.setGender(dto.gender());
        doctor.setDob(dto.dob());
        doctor.setPhone(dto.phone());
        doctor.setEmail(dto.email());
        doctor.setPrimarySpecialty(dto.primarySpecialty());
        doctor.setYearsExperience(dto.yearsExperience());
        doctor.setRegistrationNumber(dto.registrationNumber());
        doctor.setRegistrationCouncil(dto.registrationCouncil());
        doctor = doctorRepo.save(doctor);

        // doctor role active
        Role doctorRole = roleRepo.findByName("DOCTOR");
        if (doctorRole == null) {
            doctorRole = new Role();
            doctorRole.setName("DOCTOR");
            doctorRole.setDescription("Doctor role");
            doctorRole = roleRepo.save(doctorRole);
        }
        UserRole ur = new UserRole();
        ur.setUser(user);
        ur.setRole(doctorRole);
        ur.setActive(true);
        userRoleRepo.save(ur);

        return doctor.getId();
    }
}
