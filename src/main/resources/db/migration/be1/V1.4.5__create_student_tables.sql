CREATE TABLE student (
    student_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    grade VARCHAR(20) NOT NULL,
    school VARCHAR(100),
    phone VARCHAR(30),
    parent_phone VARCHAR(30),
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (student_id),
    INDEX idx_student_academy_name (academy_id, name, student_id)
);

CREATE TABLE student_enrollment (
    enrollment_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (enrollment_id),
    CONSTRAINT uk_student_enrollment_student_lecture UNIQUE (academy_id, student_id, lecture_id),
    CONSTRAINT fk_student_enrollment_student FOREIGN KEY (student_id) REFERENCES student (student_id),
    INDEX idx_student_enrollment_student_status (academy_id, student_id, status),
    INDEX idx_student_enrollment_lecture_status (academy_id, lecture_id, status)
);
