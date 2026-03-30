package com.Project.Hospital_Management_System.service;

import com.Project.Hospital_Management_System.dto.AvailabilitySlotDto;
import com.Project.Hospital_Management_System.entity.Appointment;
import com.Project.Hospital_Management_System.entity.DoctorSchedule;
import com.Project.Hospital_Management_System.repository.AppointmentRepository;
import com.Project.Hospital_Management_System.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class AvailabilityService {

    private final DoctorScheduleRepository scheduleRepo;
    private final AppointmentRepository appointmentRepo;

    public AvailabilityService(DoctorScheduleRepository scheduleRepo,
                               AppointmentRepository appointmentRepo) {
        this.scheduleRepo = scheduleRepo;
        this.appointmentRepo = appointmentRepo;
    }

    public List<AvailabilitySlotDto> getAvailability(Long doctorId, LocalDate date, ZoneId zone) {
        // Get active schedules for the given day
        List<DoctorSchedule> schedules = scheduleRepo
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, date.getDayOfWeek());
        if (schedules.isEmpty()) return List.of();

        // Generate all possible time slots
        List<AvailabilitySlotDto> allSlots = new ArrayList<>();
        for (DoctorSchedule s : schedules) {
            int dur = Math.max(10, Optional.ofNullable(s.getSlotDurationMinutes()).orElse(15));
            OffsetDateTime start = date.atTime(s.getStartTime()).atZone(zone).toOffsetDateTime();
            OffsetDateTime end = date.atTime(s.getEndTime()).atZone(zone).toOffsetDateTime();

            OffsetDateTime cur = start;
            while (!cur.plusMinutes(dur).isAfter(end)) {
                allSlots.add(new AvailabilitySlotDto(cur, cur.plusMinutes(dur)));
                cur = cur.plusMinutes(dur);
            }
        }

        // Get existing appointments for the day
        OffsetDateTime dayStart = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<Appointment> booked = appointmentRepo.findByDoctorId(doctorId).stream()
                .filter(a -> Appointment.STATUS_PENDING.equals(a.getStatus())
                        || Appointment.STATUS_CONFIRMED.equals(a.getStatus()))
                .filter(a -> a.getStartTime().isBefore(dayEnd) && a.getEndTime().isAfter(dayStart))
                .toList();

        // Filter out booked slots
        return allSlots.stream()
                .filter(slot -> booked.stream().noneMatch(a ->
                        a.getStartTime().isBefore(slot.endTime())
                                && a.getEndTime().isAfter(slot.startTime())))
                .sorted(Comparator.comparing(AvailabilitySlotDto::startTime))
                .toList();
    }
}
