package com.Project.Hospital_Management_System.Entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    // ── Payment method constants (no enum, matching project convention) ──
    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_CARD = "CARD";
    public static final String METHOD_UPI = "UPI";
    public static final String METHOD_INSURANCE = "INSURANCE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(nullable = false, length = 20)
    private String method;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "reference_no")
    private String referenceNo;

    @PrePersist
    void onCreate() {
        if (this.paidAt == null) {
            this.paidAt = OffsetDateTime.now();
        }
    }

    // ── Constructors ──

    public Payment() {
    }

    // ── Getters / Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }
}
