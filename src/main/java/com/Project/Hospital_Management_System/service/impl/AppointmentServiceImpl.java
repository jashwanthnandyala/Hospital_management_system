package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.dto.AppointmentResponse;
import com.Project.Hospital_Management_System.dto.CreateAppointmentRequest;
import com.Project.Hospital_Management_System.dto.UpdateStatusResponse;
import com.Project.Hospital_Management_System.entity.Appointment;
import com.Project.Hospital_Management_System.entity.DoctorSchedule;
import com.Project.Hospital_Management_System.exception.BadRequestException;
import com.Project.Hospital_Management_System.exception.ConflictException;
import com.Project.Hospital_Management_System.exception.NotFoundException;
import com.Project.Hospital_Management_System.repository.AppointmentRepository;
import com.Project.Hospital_Management_System.repository.DoctorRepository;
import com.Project.Hospital_Management_System.repository.DoctorScheduleRepository;
import com.Project.Hospital_Management_System.repository.PatientRepository;
import com.Project.Hospital_Management_System.service.AppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository apptRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final DoctorScheduleRepository scheduleRepo;
    private final ZoneId zone = ZoneId.of("UTC");

    public AppointmentServiceImpl(AppointmentRepository apptRepo,
                                  DoctorRepository doctorRepo,
                                  PatientRepository patientRepo,
                                  DoctorScheduleRepository scheduleRepo) {
        this.apptRepo = apptRepo;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.scheduleRepo = scheduleRepo;
    }

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest req) {
        // Validate doctor exists
        if (!doctorRepo.existsById(req.doctorId()))
            throw new NotFoundException("Doctor not found");

        // Validate patient exists
        if (!patientRepo.existsById(req.patientId()))
            throw new NotFoundException("Patient not found");

        OffsetDateTime start = req.startTime();
        var date = start.atZoneSameInstant(zone).toLocalDate();

        // Get active schedules for that day
        var schedules = scheduleRepo.findByDoctorIdAndDayOfWeekAndActiveTrue(
                req.doctorId(), date.getDayOfWeek());
        if (schedules.isEmpty())
            throw new BadRequestException("No active schedule for that day");

        // Determine slot duration and end time
        DoctorSchedule firstSchedule = schedules.getFirst();
        int slotMinutes = Math.max(10, firstSchedule.getSlotDurationMinutes() != null
                ? firstSchedule.getSlotDurationMinutes() : 15);
        OffsetDateTime end = start.plusMinutes(slotMinutes);

        // Verify the time is within doctor's schedule
        boolean inside = schedules.stream().anyMatch(s -> {
            var sStart = date.atTime(s.getStartTime()).atZone(zone).toOffsetDateTime();
            var sEnd = date.atTime(s.getEndTime()).atZone(zone).toOffsetDateTime();
            return !start.isBefore(sStart) && !end.isAfter(sEnd);
        });
        if (!inside)
            throw new BadRequestException("Requested time not inside doctor's schedule");

        // Check for overlap
        if (apptRepo.existsOverlapping(req.doctorId(), start, end))
            throw new ConflictException("Overlaps with another appointment");

        // Create the appointment
        Appointment appt = new Appointment();
        appt.setDoctorId(req.doctorId());
        appt.setPatientId(req.patientId());
        appt.setStartTime(start);
        appt.setEndTime(end);
        appt.setReason(req.reason());
        appt.setStatus(Appointment.STATUS_PENDING);

        appt = apptRepo.save(appt);
        return toResponse(appt);
    }

    @Override
    public List<AppointmentResponse> search(Long patientId, Long doctorId, String status) {
        return apptRepo.search(patientId, doctorId, status)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public UpdateStatusResponse confirm(Long id) {
        Appointment a = apptRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (Appointment.STATUS_CANCELLED.equals(a.getStatus()) || Appointment.STATUS_COMPLETED.equals(a.getStatus()))
            throw new BadRequestException("Cannot confirm cancelled/completed appointment");
        a.setStatus(Appointment.STATUS_CONFIRMED);
        apptRepo.save(a);
        return new UpdateStatusResponse(a.getId(), a.getStatus());
    }

    @Override
    @Transactional
    public UpdateStatusResponse cancel(Long id) {
        Appointment a = apptRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (Appointment.STATUS_COMPLETED.equals(a.getStatus()))
            throw new BadRequestException("Cannot cancel completed appointment");
        a.setStatus(Appointment.STATUS_CANCELLED);
        apptRepo.save(a);
        return new UpdateStatusResponse(a.getId(), a.getStatus());
    }

    @Override
    @Transactional
    public UpdateStatusResponse complete(Long id) {
        Appointment a = apptRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (!Appointment.STATUS_CONFIRMED.equals(a.getStatus()))
            throw new BadRequestException("Only confirmed appointments can be completed");
        a.setStatus(Appointment.STATUS_COMPLETED);
        apptRepo.save(a);
        return new UpdateStatusResponse(a.getId(), a.getStatus());
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(), a.getPatientId(), a.getDoctorId(),
                a.getStartTime(), a.getEndTime(), a.getStatus(), a.getReason()
        );
    }
}
