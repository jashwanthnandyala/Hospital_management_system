package com.Project.Hospital_Management_System.config;

import com.Project.Hospital_Management_System.entity.Role;
import com.Project.Hospital_Management_System.entity.User;
import com.Project.Hospital_Management_System.entity.UserRole;
import com.Project.Hospital_Management_System.repository.RoleRepository;
import com.Project.Hospital_Management_System.repository.UserRepository;
import com.Project.Hospital_Management_System.repository.UserRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2) // run after SchemaInitRunner
public class BootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserRoleRepository userRoleRepo;
    private final PasswordEncoder encoder;

    public BootstrapRunner(UserRepository userRepo,
                           RoleRepository roleRepo,
                           UserRoleRepository userRoleRepo,
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // ensure roles
        Role adminRole = roleRepo.findByName("ADMIN");
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Administrator");
            adminRole = roleRepo.save(adminRole);
        }

        Role doctorRole = roleRepo.findByName("DOCTOR");
        if (doctorRole == null) {
            doctorRole = new Role();
            doctorRole.setName("DOCTOR");
            doctorRole.setDescription("Doctor role");
            doctorRole = roleRepo.save(doctorRole);
        }

        Role patientRole = roleRepo.findByName("PATIENT");
        if (patientRole == null) {
            patientRole = new Role();
            patientRole.setName("PATIENT");
            patientRole.setDescription("Patient role");
            patientRole = roleRepo.save(patientRole);
        }

        // ensure admin
        if (userRepo.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@hospital.com");
            admin.setPasswordHash(encoder.encode("Admin@123"));
            userRepo.save(admin);

            UserRole ur = new UserRole();
            ur.setUser(admin);
            ur.setRole(adminRole);
            ur.setActive(true);
            userRoleRepo.save(ur);
        }
    }
}