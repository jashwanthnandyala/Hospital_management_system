package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.DoctorRegistrationDto;
import com.Project.Hospital_Management_System.dto.DoctorUpdateDto;
import com.Project.Hospital_Management_System.entity.Doctor;
import com.Project.Hospital_Management_System.repository.DoctorRepository;
import com.Project.Hospital_Management_System.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only doctor listing + public doctor registration.
 */
@RestController
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;

    public DoctorController(DoctorRepository doctorRepository, DoctorService doctorService) {
        this.doctorRepository = doctorRepository;
        this.doctorService = doctorService;
    }

    /**
     * ADMIN: list all doctors
     */
    @GetMapping("/api/admin/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * ADMIN: get a doctor by id
     */
    @GetMapping("/api/admin/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ADMIN: update a doctor
     */
    @PutMapping("/api/admin/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id, @RequestBody DoctorUpdateDto dto) {
        return doctorRepository.findById(id).map(doctor -> {
            if (dto.fullName() != null) doctor.setFullName(dto.fullName());
            if (dto.phone() != null) doctor.setPhone(dto.phone());
            if (dto.email() != null) doctor.setEmail(dto.email());
            if (dto.primarySpecialty() != null) doctor.setPrimarySpecialty(dto.primarySpecialty());
            if (dto.yearsExperience() != null) doctor.setYearsExperience(dto.yearsExperience());
            doctorRepository.save(doctor);
            return ResponseEntity.ok(Map.of("message", "Doctor updated successfully", "doctorId", doctor.getId()));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * ADMIN: delete a doctor
     */
    @DeleteMapping("/api/admin/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        if (!doctorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        doctorRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Doctor deleted successfully"));
    }

    /**
     * PUBLIC: register a new doctor
     */
    @PostMapping("/api/doctors/register")
    public ResponseEntity<?> registerDoctor(@Valid @RequestBody DoctorRegistrationDto dto) {
        Long doctorId = doctorService.registerDoctor(dto);
        return ResponseEntity.ok(
                Map.of(
                    "doctorId", doctorId,
                    "message", "Doctor registered successfully"
                )
        );
    }
}