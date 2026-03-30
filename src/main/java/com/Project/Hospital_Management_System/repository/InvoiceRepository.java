package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatientId(Long patientId);
    List<Invoice> findByDoctorId(Long doctorId);
    List<Invoice> findByStatus(String status);
    List<Invoice> findByPatientIdAndStatus(Long patientId, String status);
    Optional<Invoice> findByAppointmentId(Long appointmentId);
}
