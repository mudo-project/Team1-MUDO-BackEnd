CREATE TABLE `attendance_message_send_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `lecture_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `entry_date` date NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_attendance_message_send_record_lecture_student_date` (`lecture_id`,`student_id`,`entry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
