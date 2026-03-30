package com.Project.Hospital_Management_System.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne @JoinColumn(name="user_id", unique = true)
    @JsonIgnore
    private User user;

    @Column(unique = true)
    private String externalId;

    @Column(nullable=false)
    private String fullName;

    private String gender;
    private LocalDate dob;
    private String phone;
    private String email;
    private String primarySpecialty;
    private Integer yearsExperience;
    private String registrationNumber;
    private String registrationCouncil;

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String approvalStatus = STATUS_PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private Long approvedBy;
    private LocalDateTime approvedAt;

    public Doctor() {}
    public Doctor(Long id) { this.id = id; }

    public Long getId() { return id;}
    public void setId(Long id) { this.id = id;}
    public User getUser() { return user;}
    public void setUser(User user) { this.user = user;}
    public String getExternalId() { return externalId;}
    public void setExternalId(String externalId) { this.externalId = externalId;}
    public String getFullName() { return fullName;}
    public void setFullName(String fullName) { this.fullName = fullName;}
    public String getGender() { return gender;}
    public void setGender(String gender) { this.gender = gender;}
    public LocalDate getDob() { return dob;}
    public void setDob(LocalDate dob) { this.dob = dob;}
    public String getPhone() { return phone;}
    public void setPhone(String phone) { this.phone = phone;}
    public String getEmail() { return email;}
    public void setEmail(String email) { this.email = email;}
    public String getPrimarySpecialty() { return primarySpecialty;}
    public void setPrimarySpecialty(String primarySpecialty) { this.primarySpecialty = primarySpecialty;}
    public Integer getYearsExperience() { return yearsExperience;}
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience;}
    public String getRegistrationNumber() { return registrationNumber;}
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber;}
    public String getRegistrationCouncil() { return registrationCouncil;}
    public void setRegistrationCouncil(String registrationCouncil) { this.registrationCouncil = registrationCouncil;}
    public String getApprovalStatus() { return approvalStatus;}
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus;}
    public String getRejectionReason() { return rejectionReason;}
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason;}
    public Long getApprovedBy() { return approvedBy;}
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy;}
    public LocalDateTime getApprovedAt() { return approvedAt;}
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt;}
}