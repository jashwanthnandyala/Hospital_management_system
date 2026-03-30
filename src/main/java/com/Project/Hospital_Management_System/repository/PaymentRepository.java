package com.Project.Hospital_Management_System.repository;

import com.Project.Hospital_Management_System.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByMethod(String method);
}
