package com.Project.Hospital_Management_System.service;

import com.Project.Hospital_Management_System.dto.InvoiceRequest;
import com.Project.Hospital_Management_System.dto.PaymentRequest;
import com.Project.Hospital_Management_System.entity.Invoice;
import com.Project.Hospital_Management_System.entity.Payment;

import java.util.List;

public interface BillingService {

    Invoice generateInvoice(Long appointmentId, InvoiceRequest request);

    Invoice getInvoiceById(Long id);

    List<Invoice> getInvoices(Long patientId, String status);

    Invoice cancelInvoice(Long invoiceId);

    Payment makePayment(PaymentRequest request);

    List<Payment> getPayments(Long invoiceId);

    Payment getPaymentById(Long id);
}
