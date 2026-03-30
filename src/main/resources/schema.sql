-- ====== Auto-generate external_id for doctors on insert ======
DROP TRIGGER IF EXISTS trg_doctors_bi_external_id;
DELIMITER $$
CREATE TRIGGER trg_doctors_bi_external_id
BEFORE INSERT ON doctors
FOR EACH ROW
BEGIN
  IF NEW.external_id IS NULL OR NEW.external_id = '' THEN
    SET @next_id := (SELECT IFNULL(MAX(id),0) + 1 FROM doctors);
    SET NEW.external_id = CONCAT('DOC-', YEAR(CURRENT_DATE), '-', LPAD(@next_id, 5, '0'));
  END IF;
END$$
DELIMITER ;

-- ====== Block appointments for unapproved doctors ======
DROP TRIGGER IF EXISTS trg_appointments_bi_doctor_approved;
DELIMITER $$
CREATE TRIGGER trg_appointments_bi_doctor_approved
BEFORE INSERT ON appointments
FOR EACH ROW
BEGIN
  DECLARE v_status VARCHAR(20);
  SELECT approval_status INTO v_status FROM doctors WHERE id = NEW.doctor_id LIMIT 1;
  IF v_status IS NULL OR v_status <> 'APPROVED' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doctor must be APPROVED before receiving appointments';
  END IF;
END$$
DELIMITER ;

-- ====== Audit logs table (if not exists) ======
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  table_name VARCHAR(120) NOT NULL,
  action ENUM('INSERT','UPDATE','DELETE') NOT NULL,
  old_value JSON NULL,
  new_value JSON NULL,
  changed_by BIGINT NULL,
  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Sample audit for doctors
DROP TRIGGER IF EXISTS trg_doctors_ai_audit;
DELIMITER $$
CREATE TRIGGER trg_doctors_ai_audit
AFTER INSERT ON doctors
FOR EACH ROW
BEGIN
  INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
  VALUES ('doctors','INSERT', NULL,
          JSON_OBJECT('id', NEW.id, 'external_id', NEW.external_id, 'full_name', NEW.full_name, 'approval_status', NEW.approval_status),
          @app_user_id);
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_doctors_au_audit;
DELIMITER $$
CREATE TRIGGER trg_doctors_au_audit
AFTER UPDATE ON doctors
FOR EACH ROW
BEGIN
  INSERT INTO audit_logs(table_name, action, old_value, new_value, changed_by)
  VALUES ('doctors','UPDATE',
          JSON_OBJECT('id', OLD.id, 'approval_status', OLD.approval_status),
          JSON_OBJECT('id', NEW.id, 'approval_status', NEW.approval_status),
          @app_user_id);
END$$
DELIMITER ;

-- ====== Procedures: approve/reject doctor & followup handling ======
DROP PROCEDURE IF EXISTS approve_doctor;
DELIMITER $$
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

  SET @app_user_id := p_admin_user_id; -- for audit
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS reject_doctor;
DELIMITER $$
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

  SET @app_user_id := p_admin_user_id; -- for audit
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS add_followup_for_patient;
DELIMITER $$
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
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS update_followup_status;
DELIMITER $$
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

  -- recompute snapshot (earliest OPEN)
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
END$$
DELIMITER ;