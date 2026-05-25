CREATE TABLE IF NOT EXISTS email_change_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  old_email VARCHAR(255) NOT NULL,
  new_email VARCHAR(255) NOT NULL,
  status ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  reviewed_by BIGINT NULL,
  pending_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'pending' THEN user_id ELSE NULL END) STORED,
  CONSTRAINT fk_email_change_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_email_change_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
  CONSTRAINT uk_email_change_pending_user UNIQUE (pending_user_id)
);
