# 🏥 Hospital Management System — Project Documentation

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Architecture & Flow Diagram](#3-project-architecture--flow-diagram)
4. [Module 1: Authentication & User Management](#4-module-1-authentication--user-management)
5. [Module 2: Doctor Management](#5-module-2-doctor-management)
6. [Module 3: Patient Management](#6-module-3-patient-management)
7. [Module 4: Appointments Management](#7-module-4-appointments-management)
8. [Module 5: Billing & Invoices](#8-module-5-billing--invoices)
9. [Database Tables Reference](#9-database-tables-reference)
10. [Entity Relationship (ER) Diagram](#10-entity-relationship-er-diagram)
11. [Stored Procedures & Triggers](#11-stored-procedures--triggers)
12. [API Endpoints Summary](#12-api-endpoints-summary)
13. [Security Configuration](#13-security-configuration)

---

## 1. Project Overview

The **Hospital Management System (HMS)** is a REST API backend built using **Spring Boot 4.0.4** and **Java 21**. It manages the end-to-end lifecycle of hospital operations including user registration & authentication, doctor onboarding with admin approval workflows, patient record management, appointment scheduling with availability checks, medical records, follow-ups, and billing with invoice generation and payment processing.

### Key Highlights

- **JWT-based stateless authentication** with role-based access control (RBAC)
- **Three user roles**: `ADMIN`, `DOCTOR`, `PATIENT`
- **Doctor approval workflow** — doctors register and must be approved by admin before operating
- **Appointment scheduling** with doctor schedule validation, overlap detection, and status lifecycle
- **Billing engine** — automatic invoice generation from doctor consultation fees with 10% tax calculation
- **Stored procedures & triggers** for complex operations, audit logging, and data integrity
- **Audit trail** — all critical operations logged to an `audit_logs` table via MySQL triggers
- **Soft-delete support** for patients

---

## 2. Technology Stack

| Layer              | Technology                                      |
|--------------------|------------------------------------------------|
| **Language**       | Java 21                                         |
| **Framework**      | Spring Boot 4.0.4                               |
| **Security**       | Spring Security + JWT (jjwt 0.12.x)            |
| **ORM**            | Spring Data JPA / Hibernate                     |
| **Database**       | MySQL 8.x                                       |
| **Build Tool**     | Maven                                           |
| **Server Port**    | 8087                                            |
| **Password Hashing** | BCrypt                                       |
| **JWT Expiry**     | 24 hours (86400000 ms)                          |

---

## 3. Project Architecture & Flow Diagram

### 3.1 Layered Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT (Postman / Frontend)              │
│                    Sends HTTP requests with JWT token            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SECURITY LAYER                                │
│  ┌──────────────────┐  ┌───────────────────┐  ┌──────────────┐ │
│  │ AuthTokenFilter   │→│ JwtUtils          │→│ CustomUser    │ │
│  │ (OncePerRequest)  │  │ (validate/parse)  │  │ DetailsService│ │
│  └──────────────────┘  └───────────────────┘  └──────────────┘ │
│  ┌──────────────────┐  ┌───────────────────┐                   │
│  │ WebSecurityConfig │  │ RequestAuditFilter│                   │
│  │ (filterChain)     │  │ (sets @app_user)  │                   │
│  └──────────────────┘  └───────────────────┘                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CONTROLLER LAYER (REST APIs)                  │
│  ┌────────────┐ ┌──────────────┐ ┌───────────────┐             │
│  │AuthController│ │AdminController│ │DoctorController│            │
│  └────────────┘ └──────────────┘ └───────────────┘             │
│  ┌─────────────────┐ ┌───────────────────┐ ┌────────────────┐  │
│  │PatientController │ │AppointmentController│ │BillingController│ │
│  └─────────────────┘ └───────────────────┘ └────────────────┘  │
│  ┌──────────────────┐ ┌────────────────────┐ ┌──────────────┐  │
│  │DoctorProfile     │ │DoctorSchedule      │ │MedicalRecord │  │
│  │Controller        │ │Controller          │ │Controller    │  │
│  └──────────────────┘ └────────────────────┘ └──────────────┘  │
│  ┌──────────────────┐ ┌────────────────┐ ┌────────────────┐    │
│  │AddressController  │ │ContactController│ │HealthController│   │
│  └──────────────────┘ └────────────────┘ └────────────────┘    │
│  ┌──────────────────────┐ ┌─────────────────────────┐          │
│  │FollowupController    │ │AvailabilityController   │          │
│  └──────────────────────┘ └─────────────────────────┘          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SERVICE LAYER (Business Logic)                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐        │
│  │DoctorService  │ │PatientService│ │AppointmentService│        │
│  └──────────────┘ └──────────────┘ └──────────────────┘        │
│  ┌──────────────┐ ┌──────────────────┐ ┌────────────────┐      │
│  │BillingService │ │AvailabilityService│ │FollowupService │     │
│  └──────────────┘ └──────────────────┘ └────────────────┘      │
│  ┌──────────────────────────┐ ┌──────────────────┐             │
│  │PatientMedicalRecordService│ │MedicalRecordService│           │
│  └──────────────────────────┘ └──────────────────┘             │
│  ┌──────────┐ ┌──────────────────────┐ ┌───────────────────┐   │
│  │AuthFacade │ │PatientAddressService │ │PatientContactService│ │
│  └──────────┘ └──────────────────────┘ └───────────────────┘   │
│  ┌───────────────────┐                                          │
│  │PatientHealthService│                                         │
│  └───────────────────┘                                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER (Spring Data JPA)            │
│  UserRepo │ RoleRepo │ UserRoleRepo │ DoctorRepo │ PatientRepo  │
│  AppointmentRepo │ InvoiceRepo │ PaymentRepo │ MedicalRecordRepo│
│  DoctorScheduleRepo │ DoctorProfessionalProfileRepo             │
│  PatientAddressRepo │ PatientContactRepo │ PatientFollowupRepo  │
│  PatientHealthProfileRepo                                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DATABASE LAYER (MySQL 8.x)                    │
│                                                                  │
│  Tables │ Stored Procedures │ Triggers │ Functions │ Audit Logs  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Application Startup Flow

```
Application Starts
       │
       ▼
┌──────────────────┐     ┌──────────────────┐
│ SchemaInitRunner  │────▶│ BootstrapRunner   │
│ (Order = 1)       │     │ (Order = 2)       │
│                   │     │                   │
│ • Create tables   │     │ • Ensure roles:   │
│ • Create triggers │     │   ADMIN, DOCTOR,  │
│ • Create stored   │     │   PATIENT         │
│   procedures      │     │ • Seed admin user │
│ • Create functions│     │   admin/Admin@123  │
└──────────────────┘     └──────────────────┘
```

### 3.3 Request Processing Flow

```
HTTP Request
     │
     ▼
AuthTokenFilter ─── Extract JWT from "Authorization: Bearer <token>"
     │
     ├── Token Valid? ──▶ Load UserDetails ──▶ Set SecurityContext
     │
     ▼
RequestAuditFilter ─── Set @app_user_id in MySQL session (for audit triggers)
     │
     ▼
Controller ─── @PreAuthorize role checks
     │
     ▼
Service ─── Business logic / Stored Procedure calls
     │
     ▼
Repository / EntityManager ─── Database interaction
     │
     ▼
MySQL Triggers fire ─── Audit logs, data validation, auto-generated IDs
     │
     ▼
HTTP Response (JSON)
```

---

## 4. Module 1: Authentication & User Management

### 4.1 Module Description

This module handles user registration, login, and JWT-based session management. The system supports three distinct roles: **ADMIN**, **DOCTOR**, and **PATIENT**, each with different access levels enforced via Spring Security's `@PreAuthorize` annotations.

### 4.2 Functionality

| Feature | Description |
|---------|-------------|
| **Patient Self-Registration** | Patients can sign up at `/api/auth/signup`. The role is hardcoded to `ROLE_PATIENT`. |
| **Doctor Registration** | Doctors register via `/api/doctors/register` with their professional details. They start in `PENDING` approval status. |
| **Admin Seeding** | The admin account (`admin`/`Admin@123`) is automatically created on application startup via `BootstrapRunner`. |
| **Login (Sign-in)** | All users (admin, doctor, patient) authenticate via `/api/auth/signin` and receive a JWT token. |
| **JWT Token** | HMAC-SHA signed token valid for 24 hours. Must be included as `Authorization: Bearer <token>` on all protected endpoints. |
| **Role-Based Access** | Endpoints are protected with `@PreAuthorize("hasRole('ADMIN')")`, `hasRole('DOCTOR')`, or `hasAnyRole(...)`. |

### 4.3 Authentication Flow

```
                    ┌──────────────────────────────────────────┐
                    │              SIGN UP (Patient)            │
                    │  POST /api/auth/signup                    │
                    │  { username, email, password }            │
                    │                                          │
                    │  ──▶ Check username uniqueness            │
                    │  ──▶ Hash password (BCrypt)               │
                    │  ──▶ Save User with role=ROLE_PATIENT     │
                    │  ──▶ Return success message               │
                    └──────────────────────────────────────────┘

                    ┌──────────────────────────────────────────┐
                    │              SIGN IN (All Roles)          │
                    │  POST /api/auth/signin                    │
                    │  { username, password }                   │
                    │                                          │
                    │  ──▶ AuthenticationManager.authenticate() │
                    │  ──▶ Load User via CustomUserDetailsService│
                    │  ──▶ Verify password (BCrypt)             │
                    │  ──▶ Generate JWT token (JwtUtils)        │
                    │  ──▶ Return { token, username, role }     │
                    └──────────────────────────────────────────┘

                    ┌──────────────────────────────────────────┐
                    │          PROTECTED API REQUEST            │
                    │  GET /api/patients/1                      │
                    │  Header: Authorization: Bearer <jwt>      │
                    │                                          │
                    │  ──▶ AuthTokenFilter extracts token       │
                    │  ──▶ JwtUtils.validateJwtToken()          │
                    │  ──▶ JwtUtils.getUsernameFromJwtToken()   │
                    │  ──▶ CustomUserDetailsService.loadByUser()│
                    │  ──▶ Set SecurityContextHolder            │
                    │  ──▶ @PreAuthorize checks role            │
                    │  ──▶ Controller processes request         │
                    └──────────────────────────────────────────┘
```

### 4.4 Tables

#### `users` Table
| Column        | Type          | Constraints              | Description                           |
|---------------|---------------|--------------------------|---------------------------------------|
| `id`          | BIGINT        | PK, AUTO_INCREMENT       | Primary key                           |
| `username`    | VARCHAR(255)  | NOT NULL, UNIQUE         | Login username                        |
| `email`       | VARCHAR(255)  | —                        | User email address                    |
| `password_hash` | VARCHAR(255)| NOT NULL                 | BCrypt-hashed password                |
| `role`        | VARCHAR(255)  | NOT NULL                 | Primary role: ROLE_ADMIN, ROLE_DOCTOR, ROLE_PATIENT |

#### `roles` Table
| Column        | Type          | Constraints              | Description                           |
|---------------|---------------|--------------------------|---------------------------------------|
| `id`          | BIGINT        | PK, AUTO_INCREMENT       | Primary key                           |
| `name`        | VARCHAR(255)  | NOT NULL, UNIQUE         | Role name: ADMIN, DOCTOR, PATIENT     |
| `description` | VARCHAR(255)  | —                        | Human-readable description            |

#### `user_roles` Table (Many-to-Many Join)
| Column        | Type          | Constraints              | Description                           |
|---------------|---------------|--------------------------|---------------------------------------|
| `id`          | BIGINT        | PK, AUTO_INCREMENT       | Primary key                           |
| `user_id`     | BIGINT        | FK → users.id, NOT NULL  | Reference to user                     |
| `role_id`     | BIGINT        | FK → roles.id, NOT NULL  | Reference to role                     |
| `is_active`   | BOOLEAN       | NOT NULL, DEFAULT true   | Whether this role assignment is active |

### 4.5 API Endpoints

| Method | Endpoint              | Auth     | Description                        |
|--------|-----------------------|----------|------------------------------------|
| POST   | `/api/auth/signup`    | Public   | Register a new patient account     |
| POST   | `/api/auth/signin`    | Public   | Login and receive JWT token        |

---

## 5. Module 2: Doctor Management

### 5.1 Module Description

This module manages the entire doctor lifecycle: self-registration, admin approval/rejection, profile management, schedule configuration, and availability querying. Doctors must be approved by an admin before they can receive appointments.

### 5.2 Functionality

| Feature | Description |
|---------|-------------|
| **Doctor Registration** | Public endpoint. Creates a User + Doctor + UserRole record. Doctor starts in `PENDING` approval status. An `external_id` (e.g., `DOC-2026-00001`) is auto-generated by MySQL trigger. |
| **Admin Approval** | Admin calls stored procedure `approve_doctor()` which sets status to `APPROVED`, activates the DOCTOR user role, and updates the user's JWT role. |
| **Admin Rejection** | Admin calls `reject_doctor()` with a reason. Deactivates the doctor's user role. |
| **Doctor Update** | Admin can update doctor details (name, phone, email, specialty, experience). |
| **Doctor Deletion** | Admin can delete a doctor and their associated user account. |
| **Professional Profile** | Doctors manage their own professional profile (education, certifications, consultation fee, languages, availability notes). |
| **Schedule Management** | Doctors/Admins create weekly schedules specifying day, start/end time, and slot duration. |
| **Availability Query** | Public-facing endpoint to get available slots for a doctor on a given date, excluding already-booked appointments. |

### 5.3 Doctor Approval Flow

```
Doctor registers
POST /api/doctors/register
       │
       ▼
┌──────────────────────┐
│ User created          │
│ Doctor created        │──── approval_status = PENDING
│ UserRole created      │──── is_active = true
│ external_id generated │──── trg_doctors_bi_external_id trigger
└──────────┬───────────┘
           │
           ▼
  Admin reviews pending doctors
  GET /api/admin/doctors
           │
     ┌─────┴─────┐
     ▼           ▼
 APPROVE       REJECT
     │           │
     ▼           ▼
POST /api/     POST /api/
admin/doctors/ admin/doctors/
{id}/approve   {id}/reject
     │           │
     ▼           ▼
┌────────────┐ ┌────────────────┐
│SP: approve │ │SP: reject      │
│_doctor()   │ │_doctor()       │
│            │ │                │
│• status =  │ │• status =      │
│  APPROVED  │ │  REJECTED      │
│• approved  │ │• rejection     │
│  _by = admin│ │  _reason = ... │
│• user role │ │• user role     │
│  activated │ │  deactivated   │
│• audit log │ │• audit log     │
└────────────┘ └────────────────┘
     │
     ▼
Doctor can now:
• Log in with ROLE_DOCTOR
• Receive appointments
• Create schedules
• Manage professional profile
```

### 5.4 Tables

#### `doctors` Table
| Column               | Type          | Constraints                 | Description                              |
|----------------------|---------------|-----------------------------|------------------------------------------|
| `id`                 | BIGINT        | PK, AUTO_INCREMENT          | Primary key                              |
| `user_id`            | BIGINT        | FK → users.id, UNIQUE       | One-to-one link to user account          |
| `external_id`        | VARCHAR(255)  | UNIQUE                      | Auto-generated: DOC-YYYY-NNNNN           |
| `full_name`          | VARCHAR(255)  | NOT NULL                    | Doctor's full name                       |
| `gender`             | VARCHAR(255)  | —                           | Gender                                   |
| `dob`                | DATE          | —                           | Date of birth                            |
| `phone`              | VARCHAR(255)  | —                           | Phone number                             |
| `email`              | VARCHAR(255)  | —                           | Email address                            |
| `primary_specialty`  | VARCHAR(255)  | —                           | E.g., Cardiology, Dermatology            |
| `years_experience`   | INT           | —                           | Years of professional experience         |
| `registration_number`| VARCHAR(255)  | —                           | Medical registration number              |
| `registration_council`| VARCHAR(255) | —                           | Issuing medical council                  |
| `approval_status`    | VARCHAR(255)  | DEFAULT 'PENDING'           | PENDING / APPROVED / REJECTED            |
| `rejection_reason`   | TEXT          | —                           | Reason if rejected                       |
| `approved_by`        | BIGINT        | —                           | Admin user ID who approved               |
| `approved_at`        | DATETIME      | —                           | Timestamp of approval                    |

#### `doctor_professional_profile` Table
| Column             | Type           | Constraints                 | Description                              |
|-------------------|----------------|-----------------------------|------------------------------------------|
| `id`              | BIGINT         | PK, AUTO_INCREMENT          | Primary key                              |
| `doctor_id`       | BIGINT         | FK → doctors.id, UNIQUE     | One-to-one link to doctor                |
| `education`       | TEXT           | —                           | Educational background                   |
| `certifications`  | TEXT           | —                           | Professional certifications              |
| `consultation_fee`| DECIMAL(10,2)  | —                           | Fee per consultation (used in billing)   |
| `languages_spoken`| VARCHAR(255)   | —                           | Languages the doctor speaks              |
| `availability_notes`| VARCHAR(255) | —                           | Free-text availability notes             |

#### `doctor_schedules` Table
| Column                 | Type        | Constraints                 | Description                              |
|-----------------------|-------------|-----------------------------|------------------------------------------|
| `id`                  | BIGINT      | PK, AUTO_INCREMENT          | Primary key                              |
| `doctor_id`           | BIGINT      | NOT NULL, INDEX             | Reference to doctor                      |
| `day_of_week`         | VARCHAR(10) | NOT NULL                    | MONDAY, TUESDAY, etc.                   |
| `start_time`          | TIME        | NOT NULL                    | Schedule start time                      |
| `end_time`            | TIME        | NOT NULL                    | Schedule end time                        |
| `slot_duration_minutes`| INT        | NOT NULL, DEFAULT 15        | Duration of each appointment slot        |
| `active`              | BOOLEAN     | NOT NULL, DEFAULT true      | Whether this schedule is currently active|
| `created_at`          | DATETIME    | NOT NULL                    | Record creation timestamp                |
| `updated_at`          | DATETIME    | NOT NULL                    | Last update timestamp                    |

### 5.5 API Endpoints

| Method | Endpoint                              | Auth        | Description                                 |
|--------|---------------------------------------|-------------|---------------------------------------------|
| POST   | `/api/doctors/register`               | Public      | Register a new doctor (PENDING status)       |
| GET    | `/api/admin/doctors`                  | ADMIN       | List all doctors                             |
| GET    | `/api/admin/doctors/{id}`             | ADMIN       | Get doctor by ID                             |
| PUT    | `/api/admin/doctors/{id}`             | ADMIN       | Update doctor details                        |
| POST   | `/api/admin/doctors/{id}/approve`     | ADMIN       | Approve a pending doctor                     |
| POST   | `/api/admin/doctors/{id}/reject`      | ADMIN       | Reject a doctor with reason                  |
| DELETE | `/api/admin/doctors/{id}`             | ADMIN       | Delete doctor and user account               |
| GET    | `/api/doctors/profile`                | DOCTOR      | Get own professional profile                 |
| GET    | `/api/doctors/profile/{doctorId}`     | DOCTOR      | Get profile by doctor ID                     |
| POST   | `/api/doctors/profile`                | DOCTOR      | Create professional profile                  |
| PUT    | `/api/doctors/profile`                | DOCTOR      | Update professional profile                  |
| DELETE | `/api/doctors/profile`                | DOCTOR      | Delete professional profile                  |
| POST   | `/api/schedules`                      | DOCTOR/ADMIN| Create schedule entry                        |
| GET    | `/api/schedules?doctorId=`            | DOCTOR/ADMIN| Get all schedules for a doctor               |
| GET    | `/api/schedules/active?doctorId=`     | DOCTOR/ADMIN| Get active schedules only                    |
| PUT    | `/api/schedules/{id}`                 | DOCTOR/ADMIN| Update a schedule entry                      |
| DELETE | `/api/schedules/{id}`                 | DOCTOR/ADMIN| Delete a schedule entry                      |
| GET    | `/api/availability?doctorId=&date=`   | Authenticated| Get available slots for a doctor on a date  |

---

## 6. Module 3: Patient Management

### 6.1 Module Description

This module manages patient records including demographics, addresses, emergency contacts, health profiles, medical records, and follow-ups. Most operations use **MySQL stored procedures** for data integrity. Patient records support soft-delete (setting `active = false`).

### 6.2 Functionality

| Feature | Description |
|---------|-------------|
| **Patient Creation** | Creates patient via `create_patient()` stored procedure. Auto-generates `external_id` (e.g., `PAT-2026-000001`). |
| **Patient Update** | Updates demographics via `update_patient()` stored procedure. |
| **Patient Soft-Delete** | Sets `active = false` via `delete_patient()` stored procedure. |
| **Address Management** | CRUD for patient addresses. Supports primary address designation (auto-unsets other primaries). |
| **Contact Management** | CRUD for emergency contacts with primary contact support. |
| **Health Profile** | Upsert health profile (height, weight, BMI auto-calculated, chronic conditions, medications). BMI is auto-recalculated by trigger on update. |
| **Medical Records** | Create/update/delete medical records linked to appointments. Stored procedures used. |
| **Follow-ups** | Doctors create follow-ups for patients. Status lifecycle: OPEN → DONE/CANCELLED/RESCHEDULED. Earliest OPEN follow-up is snapshot on patient record. |

### 6.3 Patient Management Flow

```
┌───────────────────────────────────────────────────────┐
│                    PATIENT LIFECYCLE                    │
│                                                        │
│  Create Patient ──▶ Add Address(es) ──▶ Add Contact(s) │
│       │                                                │
│       ▼                                                │
│  Save Health Profile ──▶ BMI auto-calculated           │
│       │                                                │
│       ▼                                                │
│  Book Appointment ──▶ Doctor Visit ──▶ Medical Record  │
│       │                                                │
│       ▼                                                │
│  Follow-up Created ──▶ next_followup_date on patient   │
│       │                                                │
│       ▼                                                │
│  Follow-up DONE ──▶ Recompute next followup snapshot   │
│       │                                                │
│       ▼                                                │
│  Update / Soft-Delete Patient                          │
└───────────────────────────────────────────────────────┘
```

### 6.4 Tables

#### `patients` Table
| Column                      | Type          | Constraints                    | Description                              |
|----------------------------|---------------|--------------------------------|------------------------------------------|
| `id`                       | BIGINT        | PK, AUTO_INCREMENT             | Primary key                              |
| `external_id`              | VARCHAR(50)   | NOT NULL, UNIQUE               | Auto-generated: PAT-YYYY-NNNNNN          |
| `full_name`                | VARCHAR(120)  | NOT NULL, INDEX                | Patient full name                        |
| `dob`                      | DATE          | —                              | Date of birth                            |
| `gender`                   | VARCHAR(20)   | —                              | Gender                                   |
| `phone`                    | VARCHAR(20)   | INDEX                          | Phone number                             |
| `email`                    | VARCHAR(120)  | INDEX                          | Email address                            |
| `blood_group`              | VARCHAR(5)    | —                              | Blood group (e.g., A+, O-)              |
| `active`                   | BOOLEAN       | NOT NULL, DEFAULT true         | Soft-delete flag                         |
| `created_at`               | DATETIME      | NOT NULL                       | Record creation timestamp                |
| `updated_at`               | DATETIME      | NOT NULL                       | Last update timestamp (auto-updated)     |
| `next_followup_date`       | DATE          | —                              | Snapshot: earliest OPEN follow-up date   |
| `next_followup_from_doctor_id` | BIGINT    | —                              | Snapshot: doctor for next follow-up      |

#### `patient_addresses` Table
| Column        | Type          | Constraints                     | Description                              |
|---------------|---------------|---------------------------------|------------------------------------------|
| `id`          | BIGINT        | PK, AUTO_INCREMENT              | Primary key                              |
| `patient_id`  | BIGINT        | FK → patients.id, NOT NULL, INDEX | Reference to patient                   |
| `address_type`| VARCHAR(20)   | NOT NULL                        | HOME, WORK, OTHER                        |
| `line1`       | VARCHAR(250)  | NOT NULL                        | Address line 1                           |
| `line2`       | VARCHAR(250)  | —                               | Address line 2                           |
| `city`        | VARCHAR(100)  | NOT NULL, INDEX                 | City                                     |
| `state`       | VARCHAR(100)  | NOT NULL, INDEX                 | State                                    |
| `postal_code` | VARCHAR(20)   | NOT NULL, INDEX                 | Postal/ZIP code                          |
| `country`     | VARCHAR(100)  | NOT NULL                        | Country (default: IN)                    |
| `is_primary`  | BOOLEAN       | NOT NULL                        | Primary address flag                     |
| `created_at`  | DATETIME      | NOT NULL                        | Record creation timestamp                |
| `updated_at`  | DATETIME      | NOT NULL                        | Last update timestamp                    |

#### `patient_contacts` Table
| Column         | Type          | Constraints                     | Description                              |
|----------------|---------------|---------------------------------|------------------------------------------|
| `id`           | BIGINT        | PK, AUTO_INCREMENT              | Primary key                              |
| `patient_id`   | BIGINT        | FK → patients.id, NOT NULL, INDEX | Reference to patient                   |
| `name`         | VARCHAR(120)  | NOT NULL                        | Contact person's name                    |
| `relationship` | VARCHAR(50)   | NOT NULL                        | Relationship to patient                  |
| `phone`        | VARCHAR(20)   | NOT NULL, INDEX                 | Contact phone number                     |
| `email`        | VARCHAR(120)  | INDEX                           | Contact email                            |
| `is_primary`   | BOOLEAN       | NOT NULL                        | Primary contact flag                     |
| `created_at`   | DATETIME      | NOT NULL                        | Record creation timestamp                |
| `updated_at`   | DATETIME      | NOT NULL                        | Last update timestamp                    |

#### `patient_health_profile` Table
| Column                | Type           | Constraints                     | Description                              |
|----------------------|----------------|---------------------------------|------------------------------------------|
| `id`                 | BIGINT         | PK, AUTO_INCREMENT              | Primary key                              |
| `patient_id`         | BIGINT         | FK → patients.id, UNIQUE        | One-to-one link to patient               |
| `height`             | DECIMAL(5,2)   | —                               | Height in centimeters                    |
| `weight_kg`          | DECIMAL(5,2)   | —                               | Weight in kilograms                      |
| `bmi`                | DECIMAL(5,2)   | —                               | Auto-calculated BMI                      |
| `chronic_conditions` | TEXT           | —                               | Chronic conditions description           |
| `past_medical_history`| TEXT          | —                               | Past medical history                     |
| `current_medications`| TEXT           | —                               | Current medications                      |
| `last_updated_at`    | DATETIME       | NOT NULL                        | Last update timestamp                    |

#### `medical_records` Table
| Column          | Type          | Constraints              | Description                              |
|----------------|---------------|--------------------------|------------------------------------------|
| `id`           | BIGINT        | PK, AUTO_INCREMENT       | Primary key                              |
| `appointment_id`| BIGINT       | FK → appointments.id     | Link to appointment                      |
| `patient_id`   | BIGINT        | FK → patients.id         | Link to patient                          |
| `doctor_id`    | BIGINT        | FK → doctors.id          | Link to doctor                           |
| `visit_summary`| TEXT          | —                        | Summary of the visit                     |
| `diagnosis`    | VARCHAR(255)  | —                        | Diagnosis                                |
| `doctor_notes` | TEXT          | —                        | Doctor's notes                           |
| `created_at`   | DATETIME      | —                        | Record creation timestamp                |
| `updated_at`   | DATETIME      | —                        | Last update timestamp                    |

#### `patient_followups` Table
| Column          | Type          | Constraints              | Description                              |
|----------------|---------------|--------------------------|------------------------------------------|
| `id`           | BIGINT        | PK, AUTO_INCREMENT       | Primary key                              |
| `patient_id`   | BIGINT        | FK → patients.id, NOT NULL| Link to patient                         |
| `doctor_id`    | BIGINT        | FK → doctors.id, NOT NULL | Link to doctor                          |
| `appointment_id`| BIGINT       | FK → appointments.id     | Link to appointment (optional)           |
| `followup_date`| DATE          | —                        | Scheduled follow-up date                 |
| `followup_type`| VARCHAR(255)  | DEFAULT 'REVIEW'         | REVIEW / LAB / SCAN / PROCEDURE          |
| `status`       | VARCHAR(255)  | DEFAULT 'OPEN'           | OPEN / DONE / CANCELLED / RESCHEDULED    |
| `notes`        | TEXT          | —                        | Follow-up notes                          |
| `updated_at`   | DATETIME      | —                        | Last update timestamp                    |

### 6.5 API Endpoints

| Method | Endpoint                                          | Auth               | Description                                |
|--------|--------------------------------------------------|--------------------|--------------------------------------------|
| POST   | `/api/patients`                                  | PATIENT/DOCTOR/ADMIN| Create a new patient                       |
| GET    | `/api/patients/{id}`                             | PATIENT/DOCTOR/ADMIN| Get patient by ID                          |
| PUT    | `/api/patients/{id}`                             | PATIENT/DOCTOR/ADMIN| Update patient                             |
| DELETE | `/api/patients/{id}`                             | PATIENT/DOCTOR/ADMIN| Soft-delete patient                        |
| POST   | `/api/patients/{patientId}/addresses`            | PATIENT/DOCTOR/ADMIN| Add address                                |
| GET    | `/api/patients/{patientId}/addresses`            | PATIENT/DOCTOR/ADMIN| List addresses                             |
| GET    | `/api/patients/{patientId}/addresses/primary`    | PATIENT/DOCTOR/ADMIN| Get primary address                        |
| PUT    | `/api/patients/{patientId}/addresses/{addressId}`| PATIENT/DOCTOR/ADMIN| Update address                             |
| DELETE | `/api/patients/{patientId}/addresses/{addressId}`| PATIENT/DOCTOR/ADMIN| Delete address                             |
| POST   | `/api/patients/{patientId}/contacts`             | PATIENT/DOCTOR/ADMIN| Add emergency contact                      |
| GET    | `/api/patients/{patientId}/contacts`             | PATIENT/DOCTOR/ADMIN| List contacts                              |
| GET    | `/api/patients/{patientId}/contacts/primary`     | PATIENT/DOCTOR/ADMIN| Get primary contact                        |
| PUT    | `/api/patients/{patientId}/contacts/{contactId}` | PATIENT/DOCTOR/ADMIN| Update contact                             |
| DELETE | `/api/patients/{patientId}/contacts/{contactId}` | PATIENT/DOCTOR/ADMIN| Delete contact                             |
| PUT    | `/api/patients/{patientId}/health`               | PATIENT/DOCTOR/ADMIN| Save/update health profile (upsert)        |
| GET    | `/api/patients/{patientId}/health`               | PATIENT/DOCTOR/ADMIN| Get health profile                         |
| DELETE | `/api/patients/{patientId}/health`               | PATIENT/DOCTOR/ADMIN| Delete health profile                      |
| POST   | `/api/patients/{patientId}/records`              | DOCTOR/ADMIN       | Create medical record                      |
| GET    | `/api/patients/{patientId}/records`              | DOCTOR/ADMIN       | List medical records for patient           |
| GET    | `/api/records/{id}`                              | DOCTOR/ADMIN       | Get medical record by ID                   |
| PUT    | `/api/records/{id}`                              | DOCTOR/ADMIN       | Update medical record                      |
| GET    | `/api/records/by-appointment/{appointmentId}`    | DOCTOR/ADMIN       | Get record by appointment                  |
| DELETE | `/api/patients/{patientId}/records/{recordId}`   | DOCTOR/ADMIN       | Delete medical record                      |
| GET    | `/api/doctors/medical-records/patient/{patientId}`| DOCTOR            | Doctor queries own patient's records       |
| GET    | `/api/patients/{patientId}/followups`            | PATIENT/DOCTOR/ADMIN| List follow-ups (patient-facing, read-only)|
| GET    | `/api/patients/{patientId}/followups/{followupId}`| PATIENT/DOCTOR/ADMIN| Get single follow-up                      |
| POST   | `/api/doctors/followups/patient/{patientId}`     | DOCTOR             | Create follow-up                           |
| GET    | `/api/doctors/followups/patient/{patientId}`     | DOCTOR             | List follow-ups for patient                |
| GET    | `/api/doctors/followups/{followupId}`            | DOCTOR             | Get follow-up by ID                        |
| PATCH  | `/api/doctors/followups/{followupId}`            | DOCTOR             | Update follow-up status                    |
| DELETE | `/api/doctors/followups/{followupId}`            | DOCTOR             | Delete follow-up                           |

---

## 7. Module 4: Appointments Management

### 7.1 Module Description

This module handles the complete appointment lifecycle from creation to completion. It includes schedule-aware booking, overlap detection, and a strict status transition model. A MySQL trigger blocks appointment creation for unapproved doctors.

### 7.2 Functionality

| Feature | Description |
|---------|-------------|
| **Create Appointment** | Validates doctor/patient existence, checks doctor's active schedule for the day, ensures time falls within schedule, checks for overlapping appointments, then creates with `PENDING` status. |
| **Confirm Appointment** | Transitions from `PENDING` → `CONFIRMED`. Cannot confirm cancelled/completed. |
| **Cancel Appointment** | Transitions from `PENDING`/`CONFIRMED` → `CANCELLED`. Cannot cancel completed. |
| **Complete Appointment** | Transitions from `CONFIRMED` → `COMPLETED`. Only confirmed appointments can be completed. |
| **Search Appointments** | Filter by patientId, doctorId, and/or status. |
| **Availability Check** | Generates all possible slots from doctor's schedule, then removes slots that overlap with existing PENDING/CONFIRMED appointments. |

### 7.3 Appointment Status Lifecycle

```
                    ┌──────────┐
                    │          │
      Create ──────▶│ PENDING  │
                    │          │
                    └────┬─────┘
                         │
                ┌────────┴────────┐
                │                 │
                ▼                 ▼
         ┌───────────┐    ┌────────────┐
         │ CONFIRMED │    │ CANCELLED  │
         │           │    │            │
         └─────┬─────┘    └────────────┘
               │                 ▲
               │                 │
          ┌────┴─────┐          │
          │          │          │
          ▼          └──────────┘
   ┌────────────┐
   │ COMPLETED  │
   │            │
   └────────────┘

   Status Transitions:
   • PENDING   → CONFIRMED  (confirm)
   • PENDING   → CANCELLED  (cancel)
   • CONFIRMED → COMPLETED  (complete)
   • CONFIRMED → CANCELLED  (cancel)
```

### 7.4 Appointment Creation Validation Flow

```
CreateAppointmentRequest { patientId, doctorId, startTime, reason }
                │
                ▼
        Doctor exists? ──── No ──▶ 404 "Doctor not found"
                │ Yes
                ▼
        Patient exists? ──── No ──▶ 404 "Patient not found"
                │ Yes
                ▼
        Active schedule for that day? ──── No ──▶ 400 "No active schedule"
                │ Yes
                ▼
        Calculate endTime = startTime + slotDurationMinutes
                │
                ▼
        Time within schedule window? ──── No ──▶ 400 "Time not inside schedule"
                │ Yes
                ▼
        Overlaps with existing? ──── Yes ──▶ 409 "Overlaps with another appointment"
                │ No
                ▼
        MySQL Trigger: Doctor APPROVED? ──── No ──▶ Error "Doctor must be APPROVED"
                │ Yes
                ▼
        ✅ Appointment created (PENDING)
```

### 7.5 Tables

#### `appointments` Table
| Column       | Type          | Constraints                    | Description                              |
|-------------|---------------|--------------------------------|------------------------------------------|
| `id`        | BIGINT        | PK, AUTO_INCREMENT             | Primary key                              |
| `patient_id`| BIGINT        | NOT NULL, INDEX                | Reference to patient                     |
| `doctor_id` | BIGINT        | NOT NULL, INDEX                | Reference to doctor                      |
| `start_time`| DATETIME      | NOT NULL                       | Appointment start time                   |
| `end_time`  | DATETIME      | NOT NULL                       | Appointment end time                     |
| `status`    | VARCHAR(20)   | NOT NULL, DEFAULT 'PENDING'    | PENDING / CONFIRMED / CANCELLED / COMPLETED |
| `reason`    | TEXT          | —                              | Reason for appointment                   |
| `created_at`| DATETIME      | NOT NULL                       | Record creation timestamp                |
| `updated_at`| DATETIME      | NOT NULL                       | Last update timestamp                    |
| `version`   | BIGINT        | DEFAULT 0                      | Optimistic locking version               |

### 7.6 API Endpoints

| Method | Endpoint                       | Auth                    | Description                          |
|--------|-------------------------------|-------------------------|--------------------------------------|
| POST   | `/api/appointments`           | DOCTOR/ADMIN/PATIENT    | Create appointment                   |
| GET    | `/api/appointments`           | DOCTOR/ADMIN/PATIENT    | Search (filter: patientId, doctorId, status) |
| PUT    | `/api/appointments/{id}/confirm` | DOCTOR/ADMIN/PATIENT | Confirm appointment                  |
| PUT    | `/api/appointments/{id}/cancel`  | DOCTOR/ADMIN/PATIENT | Cancel appointment                   |
| PUT    | `/api/appointments/{id}/complete`| DOCTOR/ADMIN/PATIENT | Complete appointment                 |
| GET    | `/api/availability`           | Authenticated           | Get available slots (doctorId, date) |

---

## 8. Module 5: Billing & Invoices

### 8.1 Module Description

This module manages the financial lifecycle of hospital visits: invoice generation from completed appointments, automatic tax calculation (10% rate), payment recording, and invoice status management. The consultation fee is sourced from the doctor's professional profile.

### 8.2 Functionality

| Feature | Description |
|---------|-------------|
| **Invoice Generation** | Generated for an appointment. Fee is auto-fetched from doctor's `consultation_fee` in their professional profile. Tax (10%) and total are auto-calculated. One invoice per appointment (duplicate check). |
| **Invoice Listing** | Filter invoices by patientId and/or status (DUE, PAID, CANCELLED). |
| **Invoice Cancellation** | Cancels a DUE invoice. Cannot cancel a PAID invoice. |
| **Payment Recording** | Records a payment against an invoice. Supports methods: CASH, CARD, UPI, INSURANCE. Automatically marks invoice as PAID. |
| **Payment Listing** | Filter payments by invoiceId. |

### 8.3 Billing Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    BILLING LIFECYCLE                          │
│                                                              │
│  Appointment COMPLETED                                       │
│       │                                                      │
│       ▼                                                      │
│  POST /api/billing/invoices/generate?appointmentId=X         │
│  Body: { patientId, doctorId }                               │
│       │                                                      │
│       ├── Check: Invoice already exists? ──▶ 409 Conflict    │
│       │                                                      │
│       ├── Fetch doctor's consultation_fee                     │
│       │   from doctor_professional_profile                   │
│       │                                                      │
│       ├── Calculate:                                         │
│       │   amount = consultationFee                           │
│       │   tax    = amount × 10%                              │
│       │   total  = amount + tax                              │
│       │                                                      │
│       ▼                                                      │
│  ┌──────────┐                                                │
│  │ INVOICE  │ status = DUE                                   │
│  │ Created  │                                                │
│  └────┬─────┘                                                │
│       │                                                      │
│  ┌────┴────────────────────┐                                 │
│  │                         │                                 │
│  ▼                         ▼                                 │
│ CANCEL                   PAY                                 │
│  │                         │                                 │
│  ▼                         ▼                                 │
│ PUT /api/billing/       POST /api/billing/payments           │
│ invoices/{id}/cancel    { invoiceId, method, amount, refNo } │
│  │                         │                                 │
│  ▼                         ├── method: CASH/CARD/UPI/INSURANCE│
│ status = CANCELLED         │                                 │
│                            ▼                                 │
│                     ┌───────────┐                            │
│                     │  PAYMENT  │                            │
│                     │  recorded │                            │
│                     └─────┬─────┘                            │
│                           │                                  │
│                           ▼                                  │
│                    Invoice status = PAID                      │
│                                                              │
│  Invoice Status Transitions:                                 │
│  • DUE → PAID      (after payment)                           │
│  • DUE → CANCELLED (admin/doctor cancels)                    │
│  • PAID → (terminal, cannot cancel)                          │
│  • CANCELLED → (terminal, cannot pay)                        │
└─────────────────────────────────────────────────────────────┘
```

### 8.4 Tables

#### `invoices` Table
| Column          | Type           | Constraints                    | Description                              |
|----------------|----------------|--------------------------------|------------------------------------------|
| `id`           | BIGINT         | PK, AUTO_INCREMENT             | Primary key                              |
| `appointment_id`| BIGINT        | —                              | Link to appointment                      |
| `patient_id`   | BIGINT         | NOT NULL                       | Link to patient                          |
| `doctor_id`    | BIGINT         | —                              | Link to doctor                           |
| `amount`       | DECIMAL(10,2)  | —                              | Base consultation fee                    |
| `tax`          | DECIMAL(10,2)  | —                              | Tax amount (10% of amount)               |
| `total`        | DECIMAL(10,2)  | —                              | Total = amount + tax                     |
| `status`       | VARCHAR(20)    | NOT NULL, DEFAULT 'DUE'        | DUE / PAID / CANCELLED                   |
| `created_at`   | DATETIME       | —                              | Invoice creation timestamp               |
| `updated_at`   | DATETIME       | —                              | Last update timestamp                    |

#### `payments` Table
| Column        | Type           | Constraints              | Description                              |
|--------------|----------------|--------------------------|------------------------------------------|
| `id`         | BIGINT         | PK, AUTO_INCREMENT       | Primary key                              |
| `invoice_id` | BIGINT         | NOT NULL                 | Link to invoice                          |
| `method`     | VARCHAR(20)    | NOT NULL                 | CASH / CARD / UPI / INSURANCE            |
| `amount`     | DECIMAL(10,2)  | —                        | Payment amount                           |
| `paid_at`    | DATETIME       | —                        | Payment timestamp (auto-set)             |
| `reference_no`| VARCHAR(255)  | —                        | Transaction reference number             |

### 8.5 API Endpoints

| Method | Endpoint                                  | Auth            | Description                              |
|--------|------------------------------------------|-----------------|------------------------------------------|
| POST   | `/api/billing/invoices/generate?appointmentId=` | DOCTOR/ADMIN | Generate invoice for appointment       |
| GET    | `/api/billing/invoices`                  | All Authenticated | List invoices (filter: patientId, status) |
| GET    | `/api/billing/invoices/{id}`             | All Authenticated | Get invoice by ID                       |
| PUT    | `/api/billing/invoices/{id}/cancel`      | DOCTOR/ADMIN    | Cancel an invoice                        |
| POST   | `/api/billing/payments`                  | All Authenticated | Record a payment                        |
| GET    | `/api/billing/payments`                  | All Authenticated | List payments (filter: invoiceId)       |
| GET    | `/api/billing/payments/{id}`             | All Authenticated | Get payment by ID                       |

---

## 9. Database Tables Reference

### Complete Table Summary

| # | Table Name                    | Description                              | Key Relationships                        |
|---|------------------------------|------------------------------------------|------------------------------------------|
| 1 | `users`                      | User accounts for authentication         | Referenced by doctors, user_roles         |
| 2 | `roles`                      | Role definitions (ADMIN, DOCTOR, PATIENT)| Referenced by user_roles                  |
| 3 | `user_roles`                 | Many-to-many user ↔ role assignments     | FK → users, FK → roles                   |
| 4 | `doctors`                    | Doctor profiles & approval status        | FK → users (1:1)                          |
| 5 | `doctor_professional_profile`| Doctor education, fees, certifications   | FK → doctors (1:1)                        |
| 6 | `doctor_schedules`           | Weekly schedule templates                | doctor_id → doctors                       |
| 7 | `patients`                   | Patient demographics & soft-delete       | Referenced by addresses, contacts, etc.   |
| 8 | `patient_addresses`          | Patient address records                  | FK → patients (M:1)                       |
| 9 | `patient_contacts`           | Emergency contact records                | FK → patients (M:1)                       |
| 10| `patient_health_profile`     | Health vitals & medical history           | FK → patients (1:1)                       |
| 11| `appointments`               | Appointment bookings & status            | patient_id, doctor_id columns             |
| 12| `medical_records`            | Clinical visit records                   | FK → appointments, patients, doctors      |
| 13| `patient_followups`          | Follow-up schedules                      | FK → patients, doctors, appointments      |
| 14| `invoices`                   | Billing invoices                         | appointment_id, patient_id, doctor_id     |
| 15| `payments`                   | Payment transactions                     | invoice_id → invoices                     |
| 16| `audit_logs`                 | Audit trail for all changes              | Auto-populated by triggers                |

---

## 10. Entity Relationship (ER) Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                          ENTITY RELATIONSHIP DIAGRAM                                      │
└──────────────────────────────────────────────────────────────────────────────────────────┘

    ┌─────────┐          ┌───────────┐          ┌─────────┐
    │  users  │          │ user_roles│          │  roles  │
    │─────────│          │───────────│          │─────────│
    │ *id     │◄─── 1:M ─┤ user_id   │  M:1 ──▶│ *id     │
    │ username│          │ role_id   ├──────────│ name    │
    │ email   │          │ is_active │          │ descrip.│
    │ password│          └───────────┘          └─────────┘
    │ _hash   │
    │ role    │
    └────┬────┘
         │
         │ 1:1
         ▼
    ┌──────────────────┐          ┌─────────────────────────┐
    │     doctors      │          │ doctor_professional      │
    │──────────────────│          │ _profile                 │
    │ *id              │◄── 1:1 ──┤ doctor_id               │
    │ user_id (FK)     │          │ education               │
    │ external_id      │          │ certifications          │
    │ full_name        │          │ consultation_fee ◄──────┼─── Used for invoice amount
    │ gender, dob      │          │ languages_spoken        │
    │ phone, email     │          │ availability_notes      │
    │ primary_specialty│          └─────────────────────────┘
    │ years_experience │
    │ registration_no  │          ┌──────────────────────┐
    │ registration_    │          │  doctor_schedules    │
    │  council         │          │──────────────────────│
    │ approval_status  │◄── 1:M ──┤ doctor_id            │
    │ rejection_reason │          │ day_of_week          │
    │ approved_by      │          │ start_time           │
    │ approved_at      │          │ end_time             │
    └────────┬─────────┘          │ slot_duration_minutes│
             │                     │ active               │
             │                     └──────────────────────┘
             │
             │                              ┌──────────────────────────────┐
             │                              │        patients              │
             │                              │──────────────────────────────│
             │                              │ *id                          │
             │                              │ external_id                  │
             │                              │ full_name                    │
             │                              │ dob, gender                  │
             │                              │ phone, email                 │
             │                              │ blood_group                  │
             │                              │ active                       │
             │                              │ next_followup_date           │
             │                              │ next_followup_from_doctor_id │
             │                              └─────────┬────────────────────┘
             │                                        │
             │         ┌──────────────────────────────┼─────────────────────────┐
             │         │                              │                         │
             │         ▼                              ▼                         ▼
             │   ┌────────────────┐    ┌─────────────────────┐   ┌──────────────────────┐
             │   │patient_addresses│    │ patient_contacts     │   │patient_health_profile│
             │   │────────────────│    │─────────────────────│   │──────────────────────│
             │   │ patient_id (FK)│    │ patient_id (FK)      │   │ patient_id (FK, UQ)  │
             │   │ address_type   │    │ name                 │   │ height               │
             │   │ line1, line2   │    │ relationship         │   │ weight_kg            │
             │   │ city, state    │    │ phone, email         │   │ bmi (auto-calculated)│
             │   │ postal_code    │    │ is_primary           │   │ chronic_conditions   │
             │   │ country        │    └─────────────────────┘   │ past_medical_history │
             │   │ is_primary     │                               │ current_medications  │
             │   └────────────────┘                               └──────────────────────┘
             │
             │          ┌────────────────────────────────────────────────┐
             │          │              appointments                      │
             │          │────────────────────────────────────────────────│
             ├── M:1 ──▶│ *id                                           │
             │          │ patient_id ◄─── M:1 ─── patients.id           │
  doctors.id │          │ doctor_id  ◄─── M:1 ─── doctors.id            │
  ───── M:1 ▶│          │ start_time                                    │
             │          │ end_time                                      │
             │          │ status (PENDING/CONFIRMED/CANCELLED/COMPLETED)│
             │          │ reason                                        │
             │          │ version (optimistic lock)                     │
             │          └──────┬───────────────────────────┬────────────┘
             │                 │                           │
             │                 │ 1:1                       │ 1:M
             │                 ▼                           ▼
             │     ┌─────────────────────┐     ┌─────────────────────┐
             │     │  medical_records    │     │  patient_followups  │
             │     │─────────────────────│     │─────────────────────│
             │     │ *id                 │     │ *id                 │
             │     │ appointment_id (FK) │     │ patient_id (FK)     │
             │     │ patient_id (FK)     │     │ doctor_id (FK)      │
             │     │ doctor_id (FK)      │     │ appointment_id (FK) │
             │     │ visit_summary       │     │ followup_date       │
             │     │ diagnosis           │     │ followup_type       │
             │     │ doctor_notes        │     │ status              │
             │     └─────────────────────┘     │ notes               │
             │                                 └─────────────────────┘
             │
             │          ┌──────────────────────────────────┐
             │          │           invoices                │
             │          │──────────────────────────────────│
             ├── M:1 ──▶│ *id                              │
             │          │ appointment_id                   │
             │          │ patient_id                       │
             │          │ doctor_id                        │
             │          │ amount (from consultation_fee)   │
             │          │ tax (10% of amount)              │
             │          │ total (amount + tax)             │
             │          │ status (DUE/PAID/CANCELLED)      │
             │          └──────────┬───────────────────────┘
             │                     │
             │                     │ 1:M
             │                     ▼
             │          ┌──────────────────────────────────┐
             │          │           payments                │
             │          │──────────────────────────────────│
             │          │ *id                              │
             │          │ invoice_id (FK)                  │
             │          │ method (CASH/CARD/UPI/INSURANCE) │
             │          │ amount                           │
             │          │ paid_at                          │
             │          │ reference_no                     │
             │          └──────────────────────────────────┘


    ┌──────────────────────────────────────────────────────────────┐
    │                      audit_logs                               │
    │──────────────────────────────────────────────────────────────│
    │ *id  │ table_name │ action │ old_value │ new_value │         │
    │ changed_by │ changed_at                                      │
    │──────────────────────────────────────────────────────────────│
    │ Populated automatically by MySQL AFTER INSERT/UPDATE triggers│
    │ on: doctors, appointments, invoices, payments                │
    └──────────────────────────────────────────────────────────────┘
```

### Relationship Summary

| Relationship                          | Type    | Description                                         |
|---------------------------------------|---------|-----------------------------------------------------|
| users ↔ doctors                       | 1:1     | Each doctor has one user account                    |
| users ↔ user_roles                    | 1:M     | A user can have multiple role assignments           |
| roles ↔ user_roles                    | 1:M     | A role can be assigned to multiple users            |
| doctors ↔ doctor_professional_profile | 1:1     | Each doctor has one professional profile            |
| doctors ↔ doctor_schedules            | 1:M     | A doctor can have multiple schedule entries          |
| patients ↔ patient_addresses          | 1:M     | A patient can have multiple addresses               |
| patients ↔ patient_contacts           | 1:M     | A patient can have multiple emergency contacts      |
| patients ↔ patient_health_profile     | 1:1     | Each patient has one health profile                 |
| patients ↔ appointments               | 1:M     | A patient can have multiple appointments            |
| doctors ↔ appointments                | 1:M     | A doctor can have multiple appointments             |
| appointments ↔ medical_records        | 1:1     | Each appointment can have one medical record        |
| appointments ↔ patient_followups      | 1:M     | An appointment can generate multiple follow-ups     |
| patients ↔ medical_records            | 1:M     | A patient can have multiple medical records         |
| patients ↔ patient_followups          | 1:M     | A patient can have multiple follow-ups              |
| doctors ↔ patient_followups           | 1:M     | A doctor can create multiple follow-ups             |
| appointments ↔ invoices               | 1:1     | Each appointment has at most one invoice            |
| invoices ↔ payments                   | 1:M     | An invoice can have multiple payments               |

---

## 11. Stored Procedures & Triggers

### 11.1 Stored Procedures

| Procedure                    | Module        | Description                                                         |
|-----------------------------|---------------|---------------------------------------------------------------------|
| `approve_doctor(admin_id, doctor_id)` | Doctor | Approves doctor, activates role, sets audit user                  |
| `reject_doctor(admin_id, doctor_id, reason)` | Doctor | Rejects doctor, deactivates role, records reason          |
| `create_patient(...)`        | Patient       | Creates patient record with auto-generated external_id              |
| `update_patient(...)`        | Patient       | Updates patient demographic fields                                  |
| `delete_patient(id)`         | Patient       | Soft-deletes patient (active = 0)                                   |
| `add_patient_address(...)`   | Patient       | Adds address; handles primary flag                                  |
| `update_patient_address(...)` | Patient      | Updates address; handles primary flag                               |
| `delete_patient_address(id, patient_id)` | Patient | Deletes an address                                          |
| `add_patient_contact(...)`   | Patient       | Adds emergency contact; handles primary flag                        |
| `update_patient_contact(...)` | Patient      | Updates contact; handles primary flag                               |
| `delete_patient_contact(id, patient_id)` | Patient | Deletes a contact                                           |
| `save_health_profile(...)`   | Patient       | Upsert (insert or update) health profile with BMI auto-calculation  |
| `delete_health_profile(patient_id)` | Patient | Deletes health profile                                         |
| `add_medical_record(...)`    | Medical       | Creates medical record linked to appointment/patient                |
| `update_medical_record(...)` | Medical       | Updates medical record fields                                       |
| `delete_medical_record(record_id, patient_id)` | Medical | Deletes medical record                                 |
| `add_followup_for_patient(...)` | Followup   | Creates follow-up after verifying doctor-patient relationship       |
| `update_followup_status(...)` | Followup     | Updates follow-up status; recomputes patient's next followup        |
| `create_appointment(...)`    | Appointment   | Creates appointment with PENDING status                             |
| `confirm_appointment(id)`    | Appointment   | Confirms a pending appointment                                      |
| `cancel_appointment(id)`     | Appointment   | Cancels a pending/confirmed appointment                             |
| `complete_appointment(id)`   | Appointment   | Completes a confirmed appointment                                   |
| `create_doctor_schedule(...)` | Schedule     | Creates a schedule entry for a doctor                               |
| `generate_invoice(...)`      | Billing       | Generates invoice with tax calculation                              |
| `cancel_invoice(id)`         | Billing       | Cancels a DUE invoice (not PAID)                                    |
| `record_payment(...)`        | Billing       | Records payment and marks invoice as PAID                           |

### 11.2 Functions

| Function                           | Description                                              |
|-----------------------------------|----------------------------------------------------------|
| `generate_patient_external_id(id)` | Returns `PAT-YYYY-NNNNNN` format string                 |
| `calculate_bmi(height_cm, weight)` | Calculates BMI = weight / (height_m²)                   |

### 11.3 Triggers

| Trigger                             | Table             | Event          | Description                                           |
|-------------------------------------|-------------------|----------------|-------------------------------------------------------|
| `trg_doctors_bi_external_id`        | doctors           | BEFORE INSERT  | Auto-generates DOC-YYYY-NNNNN external_id             |
| `trg_appointments_bi_doctor_approved`| appointments     | BEFORE INSERT  | Blocks appointments for unapproved doctors            |
| `trg_doctors_ai_audit`              | doctors           | AFTER INSERT   | Logs doctor creation to audit_logs                    |
| `trg_doctors_au_audit`              | doctors           | AFTER UPDATE   | Logs doctor updates to audit_logs                     |
| `trg_patients_bi_external_id`       | patients          | BEFORE INSERT  | Auto-generates PAT-YYYY-NNNNNN external_id            |
| `trg_health_before_update_bmi`      | patient_health_profile | BEFORE UPDATE | Auto-recalculates BMI on height/weight change    |
| `trg_appointments_ai_audit`         | appointments      | AFTER INSERT   | Logs appointment creation to audit_logs               |
| `trg_appointments_au_audit`         | appointments      | AFTER UPDATE   | Logs appointment status changes to audit_logs         |
| `trg_invoices_ai_audit`             | invoices          | AFTER INSERT   | Logs invoice creation to audit_logs                   |
| `trg_invoices_au_audit`             | invoices          | AFTER UPDATE   | Logs invoice updates to audit_logs                    |
| `trg_payments_ai_audit`             | payments          | AFTER INSERT   | Logs payment recording to audit_logs                  |

---

## 12. API Endpoints Summary

### Public Endpoints (No Authentication Required)

| Method | Endpoint                 | Description                    |
|--------|-------------------------|--------------------------------|
| POST   | `/api/auth/signup`      | Patient self-registration      |
| POST   | `/api/auth/signin`      | Login (all roles)              |
| POST   | `/api/doctors/register` | Doctor registration            |

### Admin-Only Endpoints (`ROLE_ADMIN`)

| Method | Endpoint                           | Description                    |
|--------|------------------------------------|--------------------------------|
| GET    | `/api/admin/doctors`               | List all doctors               |
| GET    | `/api/admin/doctors/{id}`          | Get doctor by ID               |
| PUT    | `/api/admin/doctors/{id}`          | Update doctor                  |
| POST   | `/api/admin/doctors/{id}/approve`  | Approve doctor                 |
| POST   | `/api/admin/doctors/{id}/reject`   | Reject doctor                  |
| DELETE | `/api/admin/doctors/{id}`          | Delete doctor                  |

### Doctor-Only Endpoints (`ROLE_DOCTOR`)

| Method | Endpoint                                         | Description                    |
|--------|--------------------------------------------------|--------------------------------|
| GET    | `/api/doctors/profile`                           | Get own profile                |
| POST   | `/api/doctors/profile`                           | Create profile                 |
| PUT    | `/api/doctors/profile`                           | Update profile                 |
| DELETE | `/api/doctors/profile`                           | Delete profile                 |
| GET    | `/api/doctors/medical-records/patient/{patientId}`| Query patient records         |
| POST   | `/api/doctors/followups/patient/{patientId}`     | Create follow-up               |
| PATCH  | `/api/doctors/followups/{followupId}`            | Update follow-up               |
| DELETE | `/api/doctors/followups/{followupId}`            | Delete follow-up               |

### Multi-Role Endpoints

| Method | Endpoint                                          | Roles                   | Description                     |
|--------|--------------------------------------------------|------------------------|---------------------------------|
| POST   | `/api/patients`                                  | PATIENT, DOCTOR, ADMIN | Create patient                  |
| GET    | `/api/patients/{id}`                             | PATIENT, DOCTOR, ADMIN | Get patient                     |
| PUT    | `/api/patients/{id}`                             | PATIENT, DOCTOR, ADMIN | Update patient                  |
| DELETE | `/api/patients/{id}`                             | PATIENT, DOCTOR, ADMIN | Soft-delete patient             |
| POST   | `/api/appointments`                              | PATIENT, DOCTOR, ADMIN | Create appointment              |
| GET    | `/api/appointments`                              | PATIENT, DOCTOR, ADMIN | Search appointments             |
| POST   | `/api/billing/invoices/generate`                 | DOCTOR, ADMIN          | Generate invoice                |
| POST   | `/api/billing/payments`                          | PATIENT, DOCTOR, ADMIN | Make payment                    |
| GET    | `/api/availability`                              | Authenticated          | Check slot availability         |

---

## 13. Security Configuration

### 13.1 Security Flow

```
┌──────────────────────────────────────────────────────────┐
│                 SECURITY FILTER CHAIN                      │
│                                                           │
│  1. CSRF disabled (stateless API)                         │
│  2. Session: STATELESS (no server-side sessions)          │
│  3. Public endpoints:                                     │
│     • /api/auth/**                                        │
│     • /api/doctors/register                               │
│  4. All other endpoints: authenticated                    │
│  5. AuthTokenFilter runs before UsernamePasswordFilter    │
│  6. Method-level security: @PreAuthorize enabled          │
│  7. Password encoding: BCryptPasswordEncoder              │
└──────────────────────────────────────────────────────────┘
```

### 13.2 JWT Token Structure

| Field     | Value                                     |
|-----------|-------------------------------------------|
| Algorithm | HMAC-SHA (HS256/384/512)                  |
| Subject   | username                                  |
| Issued At | Current timestamp                         |
| Expiration| 24 hours from issuance                    |
| Secret Key| Configured in `app.jwt.secret`            |

### 13.3 Default Admin Credentials

| Field    | Value             |
|----------|-------------------|
| Username | `admin`           |
| Email    | `admin@hospital.com` |
| Password | `Admin@123`       |
| Role     | `ROLE_ADMIN`      |

---

> **Note**: This documentation is auto-generated based on source code analysis of the Hospital Management System project. For the most up-to-date information, refer to the source code in the repository.
