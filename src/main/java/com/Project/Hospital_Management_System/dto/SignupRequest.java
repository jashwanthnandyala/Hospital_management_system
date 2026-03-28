package com.Project.Hospital_Management_System.dto;

public class SignupRequest {
    private String username;
    private String email;
    private String password;
    // No role field — a user who signs up is always a PATIENT.
    // Doctors register via /api/doctors/register and admins are seeded.

    public SignupRequest() {}

    public SignupRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
