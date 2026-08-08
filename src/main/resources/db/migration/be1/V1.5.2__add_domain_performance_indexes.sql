ALTER TABLE `lecture`
  ADD INDEX `idx_lecture_academy_id` (`academy_id`, `lecture_id`),
  ADD INDEX `idx_lecture_academy_teacher` (`academy_id`, `teacher_id`),
  ADD INDEX `idx_lecture_academy_classroom` (`academy_id`, `classroom_id`);

ALTER TABLE `attendance_entry`
  ADD INDEX `idx_attendance_entry_lecture_date` (`lecture_id`, `entry_date`);

ALTER TABLE `notice`
  ADD INDEX `idx_notice_academy_pinned_created` (`academy_id`, `is_pinned`, `created_at`, `notice_id`);

ALTER TABLE `approval_document`
  ADD INDEX `idx_approval_document_academy_created` (`academy_id`, `created_at`, `approval_document_id`),
  ADD INDEX `idx_approval_document_creator_created` (`requester_user_id`, `created_at`, `approval_document_id`);

ALTER TABLE `approval_step`
  ADD INDEX `idx_approval_step_approver_status_document` (`approver_user_id`, `status`, `approval_document_id`);

ALTER TABLE `template`
  ADD INDEX `idx_template_type_academy_created` (`type`, `academy_id`, `created_at`, `template_id`);
