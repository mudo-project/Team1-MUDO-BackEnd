CREATE TABLE timetable_slot (
  timetable_slot_id BIGINT NOT NULL AUTO_INCREMENT,
  timetable_set_id BIGINT NOT NULL,
  class_type VARCHAR(20) NOT NULL,
  day_of_week VARCHAR(10) NOT NULL,
  classroom_code VARCHAR(50) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  grade VARCHAR(20),
  teacher_name VARCHAR(50),
  subject_name VARCHAR(50),
  effective_from DATE NOT NULL,
  effective_until DATE NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (timetable_slot_id),
  KEY idx_timetable_slot_set_classroom_day (timetable_set_id, classroom_code, day_of_week),
  CONSTRAINT fk_timetable_slot_set FOREIGN KEY (timetable_set_id)
    REFERENCES timetable_set (timetable_set_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE timetable_slot_exception (
  timetable_slot_id BIGINT NOT NULL,
  exception_date DATE NOT NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  class_type VARCHAR(20),
  classroom_code VARCHAR(50),
  start_time TIME,
  end_time TIME,
  grade VARCHAR(20),
  teacher_name VARCHAR(50),
  subject_name VARCHAR(50),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (timetable_slot_id, exception_date),
  CONSTRAINT fk_timetable_slot_exception_slot FOREIGN KEY (timetable_slot_id)
    REFERENCES timetable_slot (timetable_slot_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
