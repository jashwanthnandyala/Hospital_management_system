package com.Project.Hospital_Management_System.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_followups")
public class PatientFollowup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"nextFollowupDate", "nextFollowupFromDoctorId"})
    private Patient patient;

    @ManyToOne @JoinColumn(name = "doctor_id", nullable = false)
    @JsonIgnoreProperties({"user"})
    private Doctor doctor;

    @ManyToOne @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private LocalDate followupDate;

    public static final String TYPE_REVIEW    = "REVIEW";
    public static final String TYPE_LAB       = "LAB";
    public static final String TYPE_SCAN      = "SCAN";
    public static final String TYPE_PROCEDURE = "PROCEDURE";

    public static final String STATUS_OPEN        = "OPEN";
    public static final String STATUS_DONE        = "DONE";
    public static final String STATUS_CANCELLED   = "CANCELLED";
    public static final String STATUS_RESCHEDULED = "RESCHEDULED";

    private String followupType = TYPE_REVIEW;

    private String status = STATUS_OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public PatientFollowup() {
    }

    public PatientFollowup(Long id) {
        this.id = id;
    }

    // --- Getters / Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public LocalDate getFollowupDate() {
        return followupDate;
    }

    public void setFollowupDate(LocalDate followupDate) {
        this.followupDate = followupDate;
    }

    public String getFollowupType() {
        return followupType;
    }

    public void setFollowupType(String followupType) {
        this.followupType = followupType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}