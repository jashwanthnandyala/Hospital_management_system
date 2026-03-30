package com.Project.Hospital_Management_System.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs MySQL triggers, stored procedures, and DDL from schema.sql at startup.
 * Spring's sql.init cannot handle DELIMITER $$ syntax, so we execute each
 * statement individually via JdbcTemplate.
 */
@Component
@Order(1) // run before BootstrapRunner
public class SchemaInitRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public SchemaInitRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {

        // ── Fix: ensure updated_at columns have defaults on existing tables ──
        try { jdbc.execute("ALTER TABLE patients MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE patient_addresses MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE patient_contacts MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE medical_records MODIFY COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}

        // ── Audit logs table ──
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS audit_logs (
              id BIGINT PRIMARY KEY AUTO_INCREMENT,
              table_name VARCHAR(120) NOT NULL,
              action ENUM('INSERT','UPDATE','DELETE') NOT NULL,
              old_value JSON NULL,
              new_value JSON NULL,
              changed_by BIGINT NULL,
              changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // ── Trigger: auto-generate external_id for doctors ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_doctors_bi_external_id");
        jdbc.execute("""
            CREATE TRIGGER trg_doctors_bi_external_id
            BEFORE INSERT ON doctors
            FOR EACH ROW
            BEGIN
              IF NEW.external_id IS NULL OR NEW.external_id = '' THEN
                SET @next_id := (SELECT IFNULL(MAX(id),0) + 1 FROM doctors);
                SET NEW.external_id = CONCAT('DOC-', YEAR(CURRENT_DATE), '-', LPAD(@next_id, 5, '0'));
              END IF;
            END
            """);

        // ── Trigger: block appointments for unapproved doctors ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_appointments_bi_doctor_approved");
        jdbc.execute("""
            CREATE TRIGGER trg_appointments_bi_doctor_approved
            BEFORE INSERT ON appointments
            FOR EACH ROW
            BEGIN
              DECLARE v_status VARCHAR(20);
              SELECT approval_status INTO v_status FROM doctors WHERE id = NEW.doctor_id LIMIT 1;
              IF v_status IS NULL OR v_status <> 'APPROVED' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doctor must be APPROVED before receiving appointments';
              END IF;
            END
            """);

        // ── Trigger: audit after INSERT on doctors ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_doctors_ai_audit");
        jdbc.execute("""
            CREATE TRIGGER trg_doctors_ai_audit
            AFTER INSERT ON doctors
            FOR EACH ROW
            BEGIN
              INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
              VALUES ('doctors','INSERT', NULL,
                      JSON_OBJECT('id', NEW.id, 'external_id', NEW.external_id, 'full_name', NEW.full_name, 'approval_status', NEW.approval_status),
                      @app_user_id);
            END
            """);

        // ── Trigger: audit after UPDATE on doctors ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_doctors_au_audit");
        jdbc.execute("""
            CREATE TRIGGER trg_doctors_au_audit
            AFTER UPDATE ON doctors
            FOR EACH ROW
            BEGIN
              INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
              VALUES ('doctors','UPDATE',
                      JSON_OBJECT('id', OLD.id, 'approval_status', OLD.approval_status),
                      JSON_OBJECT('id', NEW.id, 'approval_status', NEW.approval_status),
                      @app_user_id);
            END
            """);

        // ── Procedure: approve_doctor ──
        jdbc.execute("DROP PROCEDURE IF EXISTS approve_doctor");
        jdbc.execute("""
            CREATE PROCEDURE approve_doctor(IN p_admin_user_id BIGINT, IN p_doctor_id BIGINT)
            BEGIN
              DECLARE v_user_id BIGINT;
              DECLARE v_role_id BIGINT;

              UPDATE doctors
                 SET approval_status = 'APPROVED',
                     approved_by = p_admin_user_id,
                     approved_at = NOW()
               WHERE id = p_doctor_id AND approval_status = 'PENDING';

              SELECT user_id INTO v_user_id FROM doctors WHERE id = p_doctor_id LIMIT 1;
              SELECT id INTO v_role_id FROM roles WHERE name = 'DOCTOR' LIMIT 1;

              UPDATE user_roles SET is_active = TRUE
               WHERE user_id = v_user_id AND role_id = v_role_id;

              SET @app_user_id := p_admin_user_id;
            END
            """);

        // ── Procedure: reject_doctor ──
        jdbc.execute("DROP PROCEDURE IF EXISTS reject_doctor");
        jdbc.execute("""
            CREATE PROCEDURE reject_doctor(IN p_admin_user_id BIGINT, IN p_doctor_id BIGINT, IN p_reason TEXT)
            BEGIN
              DECLARE v_user_id BIGINT;
              DECLARE v_role_id BIGINT;

              UPDATE doctors
                 SET approval_status = 'REJECTED',
                     rejection_reason = p_reason
               WHERE id = p_doctor_id;

              SELECT user_id INTO v_user_id FROM doctors WHERE id = p_doctor_id LIMIT 1;
              SELECT id INTO v_role_id FROM roles WHERE name = 'DOCTOR' LIMIT 1;

              UPDATE user_roles SET is_active = FALSE
               WHERE user_id = v_user_id AND role_id = v_role_id;

              SET @app_user_id := p_admin_user_id;
            END
            """);

        // ── Procedure: add_followup_for_patient ──
        jdbc.execute("DROP PROCEDURE IF EXISTS add_followup_for_patient");
        jdbc.execute("""
            CREATE PROCEDURE add_followup_for_patient(
              IN p_doctor_id BIGINT,
              IN p_patient_id BIGINT,
              IN p_appointment_id BIGINT,
              IN p_followup_date DATE,
              IN p_followup_type VARCHAR(20),
              IN p_notes TEXT
            )
            BEGIN
              DECLARE v_cnt INT DEFAULT 0;

              SELECT COUNT(1) INTO v_cnt
                FROM appointments
               WHERE doctor_id = p_doctor_id AND patient_id = p_patient_id
               LIMIT 1;

              IF v_cnt = 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doctor not authorized for this patient';
              END IF;

              INSERT INTO patient_followups(patient_id, doctor_id, appointment_id, followup_date, followup_type, notes)
              VALUES (p_patient_id, p_doctor_id, p_appointment_id, p_followup_date, UPPER(p_followup_type), p_notes);

              UPDATE patients
                 SET next_followup_date = p_followup_date,
                     next_followup_from_doctor_id = p_doctor_id
               WHERE id = p_patient_id;
            END
            """);

        // ── Procedure: update_followup_status ──
        jdbc.execute("DROP PROCEDURE IF EXISTS update_followup_status");
        jdbc.execute("""
            CREATE PROCEDURE update_followup_status(
              IN p_followup_id BIGINT,
              IN p_doctor_id BIGINT,
              IN p_status VARCHAR(20),
              IN p_new_date DATE
            )
            BEGIN
              DECLARE v_patient_id BIGINT;

              UPDATE patient_followups
                 SET status = UPPER(p_status),
                     followup_date = COALESCE(p_new_date, followup_date),
                     updated_at = NOW()
               WHERE id = p_followup_id AND doctor_id = p_doctor_id;

              SELECT patient_id INTO v_patient_id FROM patient_followups WHERE id = p_followup_id;

              UPDATE patients p
              JOIN (
                  SELECT pf.patient_id, pf.doctor_id, pf.followup_date
                    FROM patient_followups pf
                   WHERE pf.patient_id = v_patient_id AND pf.status = 'OPEN'
                   ORDER BY pf.followup_date ASC
                   LIMIT 1
              ) sub ON sub.patient_id = p.id
                 SET p.next_followup_date = sub.followup_date,
                     p.next_followup_from_doctor_id = sub.doctor_id
               WHERE p.id = v_patient_id;

              IF (SELECT COUNT(1) FROM patient_followups WHERE patient_id = v_patient_id AND status='OPEN') = 0 THEN
                 UPDATE patients SET next_followup_date = NULL, next_followup_from_doctor_id = NULL WHERE id = v_patient_id;
              END IF;
            END
            """);

        System.out.println("✅ MySQL triggers and stored procedures initialized successfully.");

        // ═══════════════════════════════════════════════════════════════
        // Patient service stored procedures (matching Hospital_ManagementSystem)
        // ═══════════════════════════════════════════════════════════════

        // ── Function: generate_patient_external_id ──
        jdbc.execute("DROP FUNCTION IF EXISTS generate_patient_external_id");
        jdbc.execute("""
            CREATE FUNCTION generate_patient_external_id(p_id BIGINT)
            RETURNS VARCHAR(50) DETERMINISTIC
            BEGIN
              RETURN CONCAT('PAT-', YEAR(CURRENT_DATE), '-', LPAD(p_id, 6, '0'));
            END
            """);

        // ── Function: calculate_bmi ──
        jdbc.execute("DROP FUNCTION IF EXISTS calculate_bmi");
        jdbc.execute("""
            CREATE FUNCTION calculate_bmi(height_cm DECIMAL(5,2), weight_kg DECIMAL(5,2))
            RETURNS DECIMAL(5,2) DETERMINISTIC
            BEGIN
              DECLARE height_m DECIMAL(5,2);
              IF height_cm IS NULL OR height_cm = 0 OR weight_kg IS NULL THEN
                RETURN NULL;
              END IF;
              SET height_m = height_cm / 100;
              RETURN ROUND(weight_kg / (height_m * height_m), 2);
            END
            """);

        // ── Procedure: create_patient ──
        jdbc.execute("DROP PROCEDURE IF EXISTS create_patient");
        jdbc.execute("""
            CREATE PROCEDURE create_patient(
              IN p_full_name VARCHAR(120), IN p_dob DATE, IN p_gender VARCHAR(20),
              IN p_phone VARCHAR(20), IN p_email VARCHAR(120), IN p_blood_group VARCHAR(5)
            )
            BEGIN
              INSERT INTO patients(external_id, full_name, dob, gender, phone, email, blood_group, active, created_at, updated_at)
              VALUES ('', p_full_name, p_dob, p_gender, p_phone, p_email, p_blood_group, 1, NOW(), NOW());

              SET @new_id = LAST_INSERT_ID();
              UPDATE patients SET external_id = generate_patient_external_id(@new_id) WHERE id = @new_id;
              SELECT @new_id;
            END
            """);

        // ── Procedure: update_patient ──
        jdbc.execute("DROP PROCEDURE IF EXISTS update_patient");
        jdbc.execute("""
            CREATE PROCEDURE update_patient(
              IN p_id BIGINT, IN p_full_name VARCHAR(120), IN p_dob DATE, IN p_gender VARCHAR(20),
              IN p_phone VARCHAR(20), IN p_email VARCHAR(120), IN p_blood_group VARCHAR(5), IN p_active TINYINT
            )
            BEGIN
              UPDATE patients SET full_name=p_full_name, dob=p_dob, gender=p_gender,
                phone=p_phone, email=p_email, blood_group=p_blood_group, active=p_active
              WHERE id = p_id;
            END
            """);

        // ── Procedure: delete_patient (soft-delete) ──
        jdbc.execute("DROP PROCEDURE IF EXISTS delete_patient");
        jdbc.execute("""
            CREATE PROCEDURE delete_patient(IN p_id BIGINT)
            BEGIN
              UPDATE patients SET active = 0 WHERE id = p_id;
            END
            """);

        // ── Procedure: add_patient_address ──
        jdbc.execute("DROP PROCEDURE IF EXISTS add_patient_address");
        jdbc.execute("""
            CREATE PROCEDURE add_patient_address(
              IN p_patient_id BIGINT, IN p_type VARCHAR(20), IN p_line1 VARCHAR(250),
              IN p_line2 VARCHAR(250), IN p_city VARCHAR(100), IN p_state VARCHAR(100),
              IN p_postal VARCHAR(20), IN p_country VARCHAR(100), IN p_primary TINYINT
            )
            BEGIN
              IF p_primary = 1 THEN
                UPDATE patient_addresses SET is_primary = 0 WHERE patient_id = p_patient_id;
              END IF;
              INSERT INTO patient_addresses(patient_id, address_type, line1, line2, city, state, postal_code, country, is_primary, created_at, updated_at)
              VALUES (p_patient_id, p_type, p_line1, p_line2, p_city, p_state, p_postal, p_country, p_primary, NOW(), NOW());
            END
            """);

        // ── Procedure: update_patient_address ──
        jdbc.execute("DROP PROCEDURE IF EXISTS update_patient_address");
        jdbc.execute("""
            CREATE PROCEDURE update_patient_address(
              IN p_address_id BIGINT, IN p_patient_id BIGINT, IN p_type VARCHAR(20),
              IN p_line1 VARCHAR(250), IN p_line2 VARCHAR(250), IN p_city VARCHAR(100),
              IN p_state VARCHAR(100), IN p_postal VARCHAR(20), IN p_country VARCHAR(100), IN p_primary TINYINT
            )
            BEGIN
              IF p_primary = 1 THEN
                UPDATE patient_addresses SET is_primary = 0 WHERE patient_id = p_patient_id AND id <> p_address_id;
              END IF;
              UPDATE patient_addresses SET address_type=p_type, line1=p_line1, line2=p_line2,
                city=p_city, state=p_state, postal_code=p_postal, country=p_country, is_primary=p_primary
              WHERE id = p_address_id AND patient_id = p_patient_id;
            END
            """);

        // ── Procedure: delete_patient_address ──
        jdbc.execute("DROP PROCEDURE IF EXISTS delete_patient_address");
        jdbc.execute("""
            CREATE PROCEDURE delete_patient_address(IN p_address_id BIGINT, IN p_patient_id BIGINT)
            BEGIN
              DELETE FROM patient_addresses WHERE id = p_address_id AND patient_id = p_patient_id;
            END
            """);

        // ── Procedure: add_patient_contact ──
        jdbc.execute("DROP PROCEDURE IF EXISTS add_patient_contact");
        jdbc.execute("""
            CREATE PROCEDURE add_patient_contact(
              IN p_patient_id BIGINT, IN p_name VARCHAR(120), IN p_relationship VARCHAR(50),
              IN p_phone VARCHAR(20), IN p_email VARCHAR(120), IN p_primary TINYINT
            )
            BEGIN
              IF p_primary = 1 THEN
                UPDATE patient_contacts SET is_primary = 0 WHERE patient_id = p_patient_id;
              END IF;
              INSERT INTO patient_contacts(patient_id, name, relationship, phone, email, is_primary, created_at, updated_at)
              VALUES (p_patient_id, p_name, p_relationship, p_phone, p_email, p_primary, NOW(), NOW());
            END
            """);

        // ── Procedure: update_patient_contact ──
        jdbc.execute("DROP PROCEDURE IF EXISTS update_patient_contact");
        jdbc.execute("""
            CREATE PROCEDURE update_patient_contact(
              IN p_contact_id BIGINT, IN p_patient_id BIGINT, IN p_name VARCHAR(120),
              IN p_relationship VARCHAR(50), IN p_phone VARCHAR(20), IN p_email VARCHAR(120), IN p_primary TINYINT
            )
            BEGIN
              IF p_primary = 1 THEN
                UPDATE patient_contacts SET is_primary = 0 WHERE patient_id = p_patient_id AND id <> p_contact_id;
              END IF;
              UPDATE patient_contacts SET name=p_name, relationship=p_relationship,
                phone=p_phone, email=p_email, is_primary=p_primary
              WHERE id = p_contact_id AND patient_id = p_patient_id;
            END
            """);

        // ── Procedure: delete_patient_contact ──
        jdbc.execute("DROP PROCEDURE IF EXISTS delete_patient_contact");
        jdbc.execute("""
            CREATE PROCEDURE delete_patient_contact(IN p_contact_id BIGINT, IN p_patient_id BIGINT)
            BEGIN
              DELETE FROM patient_contacts WHERE id = p_contact_id AND patient_id = p_patient_id;
            END
            """);

        // ── Procedure: save_health_profile (UPSERT) ──
        jdbc.execute("DROP PROCEDURE IF EXISTS save_health_profile");
        jdbc.execute("""
            CREATE PROCEDURE save_health_profile(
              IN p_patient_id BIGINT, IN p_height DECIMAL(5,2), IN p_weight DECIMAL(5,2),
              IN p_chronic TEXT, IN p_history TEXT, IN p_meds TEXT
            )
            BEGIN
              INSERT INTO patient_health_profile(patient_id, height, weight_kg, bmi, chronic_conditions, past_medical_history, current_medications, last_updated_at)
              VALUES (p_patient_id, p_height, p_weight, calculate_bmi(p_height, p_weight), p_chronic, p_history, p_meds, NOW())
              ON DUPLICATE KEY UPDATE
                height = p_height, weight_kg = p_weight, bmi = calculate_bmi(p_height, p_weight),
                chronic_conditions = p_chronic, past_medical_history = p_history,
                current_medications = p_meds, last_updated_at = NOW();
            END
            """);

        // ── Procedure: delete_health_profile ──
        jdbc.execute("DROP PROCEDURE IF EXISTS delete_health_profile");
        jdbc.execute("""
            CREATE PROCEDURE delete_health_profile(IN p_patient_id BIGINT)
            BEGIN
              DELETE FROM patient_health_profile WHERE patient_id = p_patient_id;
            END
            """);

        // ── Procedure: add_medical_record ──
        jdbc.execute("DROP PROCEDURE IF EXISTS add_medical_record");
        jdbc.execute("""
            CREATE PROCEDURE add_medical_record(
              IN p_patient_id BIGINT, IN p_appointment_id BIGINT,
              IN p_visit_summary TEXT, IN p_diagnosis VARCHAR(255), IN p_doctor_notes TEXT
            )
            BEGIN
              INSERT INTO medical_records(patient_id, appointment_id, visit_summary, diagnosis, doctor_notes, created_at, updated_at)
              VALUES (p_patient_id, p_appointment_id, p_visit_summary, p_diagnosis, p_doctor_notes, NOW(), NOW());
              SELECT LAST_INSERT_ID();
            END
            """);

        // ── Procedure: update_medical_record ──
        jdbc.execute("DROP PROCEDURE IF EXISTS update_medical_record");
        jdbc.execute("""
            CREATE PROCEDURE update_medical_record(
              IN p_id BIGINT, IN p_visit_summary TEXT, IN p_diagnosis VARCHAR(255), IN p_doctor_notes TEXT
            )
            BEGIN
              UPDATE medical_records SET visit_summary=p_visit_summary, diagnosis=p_diagnosis,
                doctor_notes=p_doctor_notes WHERE id = p_id;
            END
            """);

        // ── Procedure: delete_medical_record ──
        jdbc.execute("DROP PROCEDURE IF EXISTS delete_medical_record");
        jdbc.execute("""
            CREATE PROCEDURE delete_medical_record(IN p_record_id BIGINT, IN p_patient_id BIGINT)
            BEGIN
              DELETE FROM medical_records WHERE id = p_record_id AND patient_id = p_patient_id;
            END
            """);

        // ── Trigger: auto-generate external_id for patients ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_patients_bi_external_id");
        jdbc.execute("""
            CREATE TRIGGER trg_patients_bi_external_id
            BEFORE INSERT ON patients
            FOR EACH ROW
            BEGIN
              IF NEW.external_id IS NULL OR NEW.external_id = '' THEN
                SET @next_pid := (SELECT IFNULL(MAX(id),0) + 1 FROM patients);
                SET NEW.external_id = CONCAT('PAT-', YEAR(CURRENT_DATE), '-', LPAD(@next_pid, 6, '0'));
              END IF;
            END
            """);

        // ── Trigger: auto-recalculate BMI on health profile update ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_health_before_update_bmi");
        jdbc.execute("""
            CREATE TRIGGER trg_health_before_update_bmi
            BEFORE UPDATE ON patient_health_profile
            FOR EACH ROW
            BEGIN
              IF NEW.height <> OLD.height OR NEW.weight_kg <> OLD.weight_kg THEN
                SET NEW.bmi = calculate_bmi(NEW.height, NEW.weight_kg);
              END IF;
              SET NEW.last_updated_at = NOW();
            END
            """);

        System.out.println("✅ Patient service stored procedures initialized successfully.");

        // ═══════════════════════════════════════════════════════════════
        // Appointment service stored procedures
        // ═══════════════════════════════════════════════════════════════

        // ── Fix: ensure appointments table has all required columns ──
        // Drop old foreign key constraints from the previous ManyToOne entity design
        try {
            jdbc.execute("""
                SET @fk_name = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointments'
                    AND COLUMN_NAME = 'patient_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
                SET @sql = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE appointments DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
                PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            """);
        } catch (Exception ignored) {}
        try {
            jdbc.execute("""
                SET @fk_name = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointments'
                    AND COLUMN_NAME = 'doctor_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
                SET @sql = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE appointments DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
                PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
            """);
        } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE appointments ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE appointments ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE appointments ADD COLUMN version BIGINT DEFAULT 0"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE appointments ADD COLUMN reason TEXT"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE appointments MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'"); } catch (Exception ignored) {}
        // Migrate old status values to new enum values
        try { jdbc.execute("UPDATE appointments SET status = 'PENDING' WHERE status = 'SCHEDULED'"); } catch (Exception ignored) {}
        try { jdbc.execute("UPDATE appointments SET status = 'CANCELLED' WHERE status = 'NO_SHOW'"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE appointments MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE appointments MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        // Drop old columns that no longer exist in the entity
        try { jdbc.execute("ALTER TABLE appointments DROP COLUMN type"); } catch (Exception ignored) {}

        try { jdbc.execute("ALTER TABLE doctor_schedules MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE doctor_schedules MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}

        // ── Procedure: create_appointment ──
        jdbc.execute("DROP PROCEDURE IF EXISTS create_appointment");
        jdbc.execute("""
            CREATE PROCEDURE create_appointment(
              IN p_patient_id BIGINT, IN p_doctor_id BIGINT,
              IN p_start_time DATETIME, IN p_end_time DATETIME,
              IN p_reason TEXT
            )
            BEGIN
              INSERT INTO appointments(patient_id, doctor_id, start_time, end_time, status, reason, created_at, updated_at, version)
              VALUES (p_patient_id, p_doctor_id, p_start_time, p_end_time, 'PENDING', p_reason, NOW(), NOW(), 0);
              SELECT LAST_INSERT_ID();
            END
            """);

        // ── Procedure: confirm_appointment ──
        jdbc.execute("DROP PROCEDURE IF EXISTS confirm_appointment");
        jdbc.execute("""
            CREATE PROCEDURE confirm_appointment(IN p_id BIGINT)
            BEGIN
              UPDATE appointments SET status = 'CONFIRMED', updated_at = NOW()
               WHERE id = p_id AND status IN ('PENDING');
            END
            """);

        // ── Procedure: cancel_appointment ──
        jdbc.execute("DROP PROCEDURE IF EXISTS cancel_appointment");
        jdbc.execute("""
            CREATE PROCEDURE cancel_appointment(IN p_id BIGINT)
            BEGIN
              UPDATE appointments SET status = 'CANCELLED', updated_at = NOW()
               WHERE id = p_id AND status IN ('PENDING','CONFIRMED');
            END
            """);

        // ── Procedure: complete_appointment ──
        jdbc.execute("DROP PROCEDURE IF EXISTS complete_appointment");
        jdbc.execute("""
            CREATE PROCEDURE complete_appointment(IN p_id BIGINT)
            BEGIN
              UPDATE appointments SET status = 'COMPLETED', updated_at = NOW()
               WHERE id = p_id AND status = 'CONFIRMED';
            END
            """);

        // ── Procedure: create_doctor_schedule ──
        jdbc.execute("DROP PROCEDURE IF EXISTS create_doctor_schedule");
        jdbc.execute("""
            CREATE PROCEDURE create_doctor_schedule(
              IN p_doctor_id BIGINT, IN p_day VARCHAR(10),
              IN p_start_time TIME, IN p_end_time TIME,
              IN p_slot_minutes INT, IN p_active TINYINT
            )
            BEGIN
              INSERT INTO doctor_schedules(doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, active, created_at, updated_at)
              VALUES (p_doctor_id, p_day, p_start_time, p_end_time, p_slot_minutes, p_active, NOW(), NOW());
              SELECT LAST_INSERT_ID();
            END
            """);

        // ── Trigger: audit after INSERT on appointments ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_appointments_ai_audit");
        jdbc.execute("""
            CREATE TRIGGER trg_appointments_ai_audit
            AFTER INSERT ON appointments
            FOR EACH ROW
            BEGIN
              INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
              VALUES ('appointments','INSERT', NULL,
                      JSON_OBJECT('id', NEW.id, 'patient_id', NEW.patient_id,
                                  'doctor_id', NEW.doctor_id, 'status', NEW.status),
                      @app_user_id);
            END
            """);

        // ── Trigger: audit after UPDATE on appointments ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_appointments_au_audit");
        jdbc.execute("""
            CREATE TRIGGER trg_appointments_au_audit
            AFTER UPDATE ON appointments
            FOR EACH ROW
            BEGIN
              INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
              VALUES ('appointments','UPDATE',
                      JSON_OBJECT('id', OLD.id, 'status', OLD.status),
                      JSON_OBJECT('id', NEW.id, 'status', NEW.status),
                      @app_user_id);
            END
            """);

        System.out.println("✅ Appointment service stored procedures initialized successfully.");

        // ═══════════════════════════════════════════════════════════════
        //                 BILLING SERVICE STORED PROCEDURES
        // ═══════════════════════════════════════════════════════════════

        // ── Procedure: generate_invoice ──
        jdbc.execute("DROP PROCEDURE IF EXISTS generate_invoice");
        jdbc.execute("""
            CREATE PROCEDURE generate_invoice(
              IN p_appointment_id BIGINT,
              IN p_patient_id    BIGINT,
              IN p_doctor_id     BIGINT,
              IN p_amount        DECIMAL(10,2),
              OUT p_invoice_id   BIGINT
            )
            BEGIN
              DECLARE v_tax   DECIMAL(10,2);
              DECLARE v_total DECIMAL(10,2);

              IF p_amount IS NULL THEN
                SET p_amount = 0.00;
              END IF;

              SET v_tax   = ROUND(p_amount * 0.10, 2);
              SET v_total = p_amount + v_tax;

              INSERT INTO invoices(appointment_id, patient_id, doctor_id, amount, tax, total, status, created_at, updated_at)
              VALUES (p_appointment_id, p_patient_id, p_doctor_id, p_amount, v_tax, v_total, 'DUE', NOW(), NOW());

              SET p_invoice_id = LAST_INSERT_ID();
            END
            """);

        // ── Procedure: cancel_invoice ──
        jdbc.execute("DROP PROCEDURE IF EXISTS cancel_invoice");
        jdbc.execute("""
            CREATE PROCEDURE cancel_invoice(
              IN p_invoice_id BIGINT
            )
            BEGIN
              DECLARE v_status VARCHAR(20);

              SELECT status INTO v_status FROM invoices WHERE id = p_invoice_id;

              IF v_status IS NULL THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice not found';
              END IF;

              IF v_status = 'PAID' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot cancel a paid invoice';
              END IF;

              UPDATE invoices SET status = 'CANCELLED', updated_at = NOW() WHERE id = p_invoice_id;
            END
            """);

        // ── Procedure: record_payment ──
        jdbc.execute("DROP PROCEDURE IF EXISTS record_payment");
        jdbc.execute("""
            CREATE PROCEDURE record_payment(
              IN p_invoice_id   BIGINT,
              IN p_method       VARCHAR(20),
              IN p_amount       DECIMAL(10,2),
              IN p_reference_no VARCHAR(255),
              OUT p_payment_id  BIGINT
            )
            BEGIN
              DECLARE v_status VARCHAR(20);
              DECLARE v_total  DECIMAL(10,2);

              SELECT status, total INTO v_status, v_total FROM invoices WHERE id = p_invoice_id;

              IF v_status IS NULL THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice not found';
              END IF;

              IF v_status = 'PAID' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice is already paid';
              END IF;

              IF v_status = 'CANCELLED' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot pay a cancelled invoice';
              END IF;

              IF p_amount IS NULL THEN
                SET p_amount = v_total;
              END IF;

              INSERT INTO payments(invoice_id, method, amount, paid_at, reference_no)
              VALUES (p_invoice_id, p_method, p_amount, NOW(), p_reference_no);

              SET p_payment_id = LAST_INSERT_ID();

              UPDATE invoices SET status = 'PAID', updated_at = NOW() WHERE id = p_invoice_id;
            END
            """);

        // ── Trigger: audit after INSERT on invoices ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_invoices_ai_audit");
        jdbc.execute("""
            CREATE TRIGGER trg_invoices_ai_audit
            AFTER INSERT ON invoices
            FOR EACH ROW
            BEGIN
              INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
              VALUES ('invoices','INSERT', NULL,
                      JSON_OBJECT('id', NEW.id, 'patient_id', NEW.patient_id,
                                  'doctor_id', NEW.doctor_id, 'amount', NEW.amount,
                                  'total', NEW.total, 'status', NEW.status),
                      @app_user_id);
            END
            """);

        // ── Trigger: audit after UPDATE on invoices ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_invoices_au_audit");
        jdbc.execute("""
            CREATE TRIGGER trg_invoices_au_audit
            AFTER UPDATE ON invoices
            FOR EACH ROW
            BEGIN
              INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
              VALUES ('invoices','UPDATE',
                      JSON_OBJECT('id', OLD.id, 'status', OLD.status, 'total', OLD.total),
                      JSON_OBJECT('id', NEW.id, 'status', NEW.status, 'total', NEW.total),
                      @app_user_id);
            END
            """);

        // ── Trigger: audit after INSERT on payments ──
        jdbc.execute("DROP TRIGGER IF EXISTS trg_payments_ai_audit");
        jdbc.execute("""
            CREATE TRIGGER trg_payments_ai_audit
            AFTER INSERT ON payments
            FOR EACH ROW
            BEGIN
              INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
              VALUES ('payments','INSERT', NULL,
                      JSON_OBJECT('id', NEW.id, 'invoice_id', NEW.invoice_id,
                                  'method', NEW.method, 'amount', NEW.amount),
                      @app_user_id);
            END
            """);

        System.out.println("✅ Billing service stored procedures initialized successfully.");
    }
}
