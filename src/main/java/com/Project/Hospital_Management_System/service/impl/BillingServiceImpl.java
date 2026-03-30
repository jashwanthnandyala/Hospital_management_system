package com.Project.Hospital_Management_System.service.impl;

import com.Project.Hospital_Management_System.dto.InvoiceRequest;
import com.Project.Hospital_Management_System.dto.PaymentRequest;
import com.Project.Hospital_Management_System.entity.DoctorProfessionalProfile;
import com.Project.Hospital_Management_System.entity.Invoice;
import com.Project.Hospital_Management_System.entity.Payment;
import com.Project.Hospital_Management_System.exception.BadRequestException;
import com.Project.Hospital_Management_System.exception.ConflictException;
import com.Project.Hospital_Management_System.exception.NotFoundException;
import com.Project.Hospital_Management_System.repository.DoctorProfessionalProfileRepository;
import com.Project.Hospital_Management_System.repository.InvoiceRepository;
import com.Project.Hospital_Management_System.repository.PaymentRepository;
import com.Project.Hospital_Management_System.service.BillingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class BillingServiceImpl implements BillingService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final DoctorProfessionalProfileRepository profileRepository;

    public BillingServiceImpl(InvoiceRepository invoiceRepository,
                              PaymentRepository paymentRepository,
                              DoctorProfessionalProfileRepository profileRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public Invoice generateInvoice(Long appointmentId, InvoiceRequest request) {
        if (request.getPatientId() == null) {
            throw new BadRequestException("Patient ID is required");
        }
        if (request.getDoctorId() == null) {
            throw new BadRequestException("Doctor ID is required");
        }

        // Check for duplicate invoice per appointment
        invoiceRepository.findByAppointmentId(appointmentId).ifPresent(existing -> {
            throw new ConflictException("Invoice already exists for appointment: " + appointmentId);
        });

        // Fetch doctor's consultation fee from professional profile
        DoctorProfessionalProfile profile = profileRepository.findByDoctorId(request.getDoctorId())
                .orElseThrow(() -> new NotFoundException(
                        "Professional profile not found for doctor: " + request.getDoctorId()));

        BigDecimal amount = profile.getConsultationFee();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Doctor (id=" + request.getDoctorId() + ") has no consultation fee configured");
        }

        Invoice invoice = new Invoice();
        invoice.setAppointmentId(appointmentId);
        invoice.setPatientId(request.getPatientId());
        invoice.setDoctorId(request.getDoctorId());
        invoice.setStatus(Invoice.STATUS_DUE);
        invoice.setAmount(amount);

        // Calculate tax and total
        BigDecimal tax = amount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        invoice.setTax(tax);
        invoice.setTotal(amount.add(tax));

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoices(Long patientId, String status) {
        if (patientId != null && status != null) {
            return invoiceRepository.findByPatientIdAndStatus(patientId, status.toUpperCase());
        } else if (patientId != null) {
            return invoiceRepository.findByPatientId(patientId);
        } else if (status != null) {
            return invoiceRepository.findByStatus(status.toUpperCase());
        }
        return invoiceRepository.findAll();
    }

    @Override
    public Invoice cancelInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        if (Invoice.STATUS_PAID.equals(invoice.getStatus())) {
            throw new BadRequestException("Cannot cancel a paid invoice");
        }
        invoice.setStatus(Invoice.STATUS_CANCELLED);
        return invoiceRepository.save(invoice);
    }

    @Override
    public Payment makePayment(PaymentRequest request) {
        Invoice invoice = getInvoiceById(request.getInvoiceId());

        if (Invoice.STATUS_PAID.equals(invoice.getStatus())) {
            throw new BadRequestException("Invoice is already paid");
        }
        if (Invoice.STATUS_CANCELLED.equals(invoice.getStatus())) {
            throw new BadRequestException("Cannot pay a cancelled invoice");
        }

        Payment payment = new Payment();
        payment.setInvoiceId(request.getInvoiceId());
        payment.setMethod(request.getMethod() != null ? request.getMethod().toUpperCase() : Payment.METHOD_CASH);
        payment.setAmount(request.getAmount() != null ? request.getAmount() : invoice.getTotal());
        payment.setReferenceNo(request.getReferenceNo());

        Payment saved = paymentRepository.save(payment);

        // Mark invoice as paid
        invoice.setStatus(Invoice.STATUS_PAID);
        invoiceRepository.save(invoice);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPayments(Long invoiceId) {
        if (invoiceId != null) {
            return paymentRepository.findByInvoiceId(invoiceId);
        }
        return paymentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
    }
}
