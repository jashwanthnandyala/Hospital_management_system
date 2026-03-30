package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
           select (count(a) > 0) from Appointment a
           where a.doctorId = :doctorId
             and a.status in ('PENDING', 'CONFIRMED')
             and (a.startTime < :endTime and a.endTime > :startTime)
           """)
    boolean existsOverlapping(@Param("doctorId") Long doctorId,
                              @Param("startTime") OffsetDateTime startTime,
                              @Param("endTime") OffsetDateTime endTime);

    @Query("""
           select a from Appointment a
           where (:patientId is null or a.patientId = :patientId)
             and (:doctorId  is null or a.doctorId  = :doctorId)
             and (:status    is null or a.status    = :status)
           order by a.startTime desc
           """)
    List<Appointment> search(@Param("patientId") Long patientId,
                             @Param("doctorId") Long doctorId,
                             @Param("status") String status);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByPatientId(Long patientId);

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);
}