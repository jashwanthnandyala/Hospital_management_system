package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.DoctorScheduleRequest;
import com.Project.Hospital_Management_System.entity.DoctorSchedule;
import com.Project.Hospital_Management_System.exception.NotFoundException;
import com.Project.Hospital_Management_System.repository.DoctorScheduleRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
public class DoctorScheduleController {

    private final DoctorScheduleRepository scheduleRepo;

    public DoctorScheduleController(DoctorScheduleRepository scheduleRepo) {
        this.scheduleRepo = scheduleRepo;
    }

    /**
     * Create a new schedule entry for a doctor.
     */
    @PostMapping
    public DoctorSchedule create(@Valid @RequestBody DoctorScheduleRequest req) {
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctorId(req.doctorId());
        s.setDayOfWeek(req.dayOfWeek());
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        s.setSlotDurationMinutes(req.slotDurationMinutes() != null ? req.slotDurationMinutes() : 15);
        s.setActive(req.active() != null ? req.active() : true);
        return scheduleRepo.save(s);
    }

    /**
     * Get all schedules for a doctor.
     */
    @GetMapping
    public List<DoctorSchedule> getByDoctor(@RequestParam Long doctorId) {
        return scheduleRepo.findByDoctorId(doctorId);
    }

    /**
     * Get only active schedules for a doctor.
     */
    @GetMapping("/active")
    public List<DoctorSchedule> getActiveByDoctor(@RequestParam Long doctorId) {
        return scheduleRepo.findByDoctorIdAndActiveTrue(doctorId);
    }

    /**
     * Update a schedule entry.
     */
    @PutMapping("/{id}")
    public DoctorSchedule update(@PathVariable Long id,
                                 @Valid @RequestBody DoctorScheduleRequest req) {
        DoctorSchedule s = scheduleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Schedule not found"));
        s.setDoctorId(req.doctorId());
        s.setDayOfWeek(req.dayOfWeek());
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        if (req.slotDurationMinutes() != null) s.setSlotDurationMinutes(req.slotDurationMinutes());
        if (req.active() != null) s.setActive(req.active());
        return scheduleRepo.save(s);
    }

    /**
     * Delete a schedule entry.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!scheduleRepo.existsById(id))
            throw new NotFoundException("Schedule not found");
        scheduleRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Schedule deleted"));
    }
}
