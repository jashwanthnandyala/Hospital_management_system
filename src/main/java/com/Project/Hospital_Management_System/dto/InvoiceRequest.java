package com.Project.Hospital_Management_System.dto;

public class InvoiceRequest {
    private Long patientId;
    private Long doctorId;

    public InvoiceRequest() {}

    public InvoiceRequest(Long patientId, Long doctorId) {
        this.patientId = patientId;
        this.doctorId = doctorId;
    }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
}
