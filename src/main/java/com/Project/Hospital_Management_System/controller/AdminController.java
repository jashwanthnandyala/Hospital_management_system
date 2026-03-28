package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.entity.Doctor;
import com.Project.Hospital_Management_System.entity.User;
import com.Project.Hospital_Management_System.repository.DoctorRepository;
import com.Project.Hospital_Management_System.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints for managing doctors.
 *   GET    /api/admin/doctors              → list all doctors
 *   GET    /api/admin/doctors/{id}         → get doctor by id
 *   POST   /api/admin/doctors/{id}/approve → approve a pending doctor
 *   POST   /api/admin/doctors/{id}/reject  → reject a pending doctor
 *   DELETE /api/admin/doctors/{id}         → remove a doctor
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    public AdminController(DoctorRepository doctorRepository,
                           UserRepository userRepository,
                           JdbcTemplate jdbc) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
    }

    /** List every doctor. */
    @GetMapping("/doctors")
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    /** Get a single doctor. */
    @GetMapping("/doctors/{id}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Approve a PENDING doctor (calls the stored procedure). */
    @PostMapping("/doctors/{id}/approve")
    public ResponseEntity<?> approveDoctor(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails principal) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null) {
            return ResponseEntity.notFound().build();
        }
        if (!Doctor.STATUS_PENDING.equals(doctor.getApprovalStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Doctor is not in PENDING status"));
        }

        Long adminUserId = getAdminUserId(principal.getUsername());
        jdbc.update("CALL approve_doctor(?, ?)", adminUserId, id);

        // also set role on the doctor's User row so JWT works immediately
        User doctorUser = doctor.getUser();
        if (doctorUser != null) {
            doctorUser.setRole("ROLE_DOCTOR");
            userRepository.save(doctorUser);
        }

        return ResponseEntity.ok(Map.of("message", "Doctor approved successfully", "doctorId", id));
    }

    /** Reject a doctor with a reason. */
    @PostMapping("/doctors/{id}/reject")
    public ResponseEntity<?> rejectDoctor(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          @AuthenticationPrincipal UserDetails principal) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null) {
            return ResponseEntity.notFound().build();
        }

        String reason = body.getOrDefault("reason", "No reason provided");
        Long adminUserId = getAdminUserId(principal.getUsername());
        jdbc.update("CALL reject_doctor(?, ?, ?)", adminUserId, id, reason);

        return ResponseEntity.ok(Map.of("message", "Doctor rejected", "doctorId", id));
    }

    /** Delete (remove) a doctor and their user account. */
    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null) {
            return ResponseEntity.notFound().build();
        }
        User doctorUser = doctor.getUser();
        doctorRepository.delete(doctor);
        if (doctorUser != null) {
            userRepository.delete(doctorUser);
        }
        return ResponseEntity.ok(Map.of("message", "Doctor removed successfully"));
    }

    // ── helper ──
    private Long getAdminUserId(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElse(0L);
    }
}

