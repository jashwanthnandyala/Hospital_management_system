package com.Project.Hospital_Management_System.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private Long invoiceId;
    private String method;
    private BigDecimal amount;
    private String referenceNo;

    public PaymentRequest() {}

    public PaymentRequest(Long invoiceId, String method, BigDecimal amount, String referenceNo) {
        this.invoiceId = invoiceId;
        this.method = method;
        this.amount = amount;
        this.referenceNo = referenceNo;
    }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
}
