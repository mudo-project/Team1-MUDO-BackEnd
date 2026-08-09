CREATE TABLE timetable_set (
  timetable_set_id BIGINT NOT NULL AUTO_INCREMENT,
  academy_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  operating_start_time TIME NOT NULL,
  operating_end_time TIME NOT NULL,
  operating_days VARCHAR(50) NOT NULL,
  slot_unit_minutes INT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (timetable_set_id),
  UNIQUE KEY uk_timetable_set_academy_name (academy_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE timetable_set_classroom (
  timetable_set_id BIGINT NOT NULL,
  floor VARCHAR(50) NOT NULL,
  code VARCHAR(50) NOT NULL,
  PRIMARY KEY (timetable_set_id, code),
  CONSTRAINT fk_timetable_set_classroom_set FOREIGN KEY (timetable_set_id)
    REFERENCES timetable_set (timetable_set_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
