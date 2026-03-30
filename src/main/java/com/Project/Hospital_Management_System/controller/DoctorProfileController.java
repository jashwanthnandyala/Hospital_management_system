package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.ProfessionalProfileDto;
import com.Project.Hospital_Management_System.entity.Doctor;
import com.Project.Hospital_Management_System.entity.DoctorProfessionalProfile;
import com.Project.Hospital_Management_System.repository.DoctorProfessionalProfileRepository;
import com.Project.Hospital_Management_System.repository.DoctorRepository;
import com.Project.Hospital_Management_System.service.AuthFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CRUD for doctor_professional_profile.
 * Doctors manage their own profile; admins can view any.
 */
@RestController
@RequestMapping("/api/doctors/profile")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorProfileController {

    private final DoctorProfessionalProfileRepository profileRepo;
    private final DoctorRepository doctorRepo;
    private final AuthFacade authFacade;

    public DoctorProfileController(DoctorProfessionalProfileRepository profileRepo,
                                   DoctorRepository doctorRepo,
                                   AuthFacade authFacade) {
        this.profileRepo = profileRepo;
        this.doctorRepo = doctorRepo;
        this.authFacade = authFacade;
    }

    /**
     * GET my professional profile (current doctor)
     */
    @GetMapping
    public ResponseEntity<?> getMyProfile() {
        Long doctorId = authFacade.currentDoctorIdOrThrow();
        return profileRepo.findByDoctorId(doctorId)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.ok(Map.of("message", "No profile found. Create one.")));
    }

    /**
     * GET profile by doctor ID
     */
    @GetMapping("/{doctorId}")
    public ResponseEntity<?> getProfileByDoctorId(@PathVariable Long doctorId) {
        return profileRepo.findByDoctorId(doctorId)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST create professional profile for current doctor
     */
    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody ProfessionalProfileDto dto) {
        Long doctorId = authFacade.currentDoctorIdOrThrow();

        if (profileRepo.findByDoctorId(doctorId).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Profile already exists. Use PUT to update."));
        }

        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        DoctorProfessionalProfile profile = new DoctorProfessionalProfile();
        profile.setDoctor(doctor);
        profile.setEducation(dto.education());
        profile.setCertifications(dto.certifications());
        profile.setConsultationFee(dto.consultationFee());
        profile.setLanguagesSpoken(dto.languagesSpoken());
        profile.setAvailabilityNotes(dto.availabilityNotes());
        profileRepo.save(profile);

        return ResponseEntity.ok(Map.of(
                "profileId", profile.getId(),
                "message", "Professional profile created successfully"
        ));
    }

    /**
     * PUT update professional profile for current doctor
     */
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody ProfessionalProfileDto dto) {
        Long doctorId = authFacade.currentDoctorIdOrThrow();

        DoctorProfessionalProfile profile = profileRepo.findByDoctorId(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("No profile found. Create one first."));

        if (dto.education() != null) profile.setEducation(dto.education());
        if (dto.certifications() != null) profile.setCertifications(dto.certifications());
        if (dto.consultationFee() != null) profile.setConsultationFee(dto.consultationFee());
        if (dto.languagesSpoken() != null) profile.setLanguagesSpoken(dto.languagesSpoken());
        if (dto.availabilityNotes() != null) profile.setAvailabilityNotes(dto.availabilityNotes());
        profileRepo.save(profile);

        return ResponseEntity.ok(Map.of("message", "Professional profile updated successfully"));
    }

    /**
     * DELETE professional profile for current doctor
     */
    @DeleteMapping
    public ResponseEntity<?> deleteProfile() {
        Long doctorId = authFacade.currentDoctorIdOrThrow();

        DoctorProfessionalProfile profile = profileRepo.findByDoctorId(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("No profile found"));

        profileRepo.delete(profile);
        return ResponseEntity.ok(Map.of("message", "Professional profile deleted successfully"));
    }
}
