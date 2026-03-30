package com.Project.Hospital_Management_System.controller;

import com.Project.Hospital_Management_System.dto.InvoiceRequest;
import com.Project.Hospital_Management_System.dto.PaymentRequest;
import com.Project.Hospital_Management_System.entity.Invoice;
import com.Project.Hospital_Management_System.entity.Payment;
import com.Project.Hospital_Management_System.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN','PATIENT')")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    // ── Invoice Endpoints ──

    /**
     * Generate an invoice for an appointment.
     */
    @PostMapping("/invoices/generate")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<Invoice> generateInvoice(
            @RequestParam Long appointmentId,
            @RequestBody InvoiceRequest request) {
        Invoice invoice = billingService.generateInvoice(appointmentId, request);
        return new ResponseEntity<>(invoice, HttpStatus.CREATED);
    }

    /**
     * List invoices with optional filters (patientId, status).
     */
    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> getInvoices(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status) {
        List<Invoice> invoices = billingService.getInvoices(patientId, status);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get a single invoice by ID.
     */
    @GetMapping("/invoices/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
        Invoice invoice = billingService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Cancel an invoice (not allowed if already PAID).
     */
    @PutMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<Invoice> cancelInvoice(@PathVariable Long id) {
        Invoice invoice = billingService.cancelInvoice(id);
        return ResponseEntity.ok(invoice);
    }

    // ── Payment Endpoints ──

    /**
     * Record a payment against an invoice.
     */
    @PostMapping("/payments")
    public ResponseEntity<Payment> makePayment(@RequestBody PaymentRequest request) {
        Payment payment = billingService.makePayment(request);
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }

    /**
     * List payments with optional invoiceId filter.
     */
    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getPayments(
            @RequestParam(required = false) Long invoiceId) {
        List<Payment> payments = billingService.getPayments(invoiceId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get a single payment by ID.
     */
    @GetMapping("/payments/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        Payment payment = billingService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }
}
