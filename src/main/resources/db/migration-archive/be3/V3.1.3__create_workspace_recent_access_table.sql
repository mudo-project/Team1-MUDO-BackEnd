CREATE TABLE `workspace_recent_access` (
                                           `user_id` BIGINT      NOT NULL,
                                           `workspace_id` BIGINT NOT NULL,
                                           `last_accessed_at` DATETIME(6) NOT NULL,
                                           PRIMARY KEY (`user_id`, `workspace_id`),
                                           KEY `idx_workspace_recent_access_user_accessed`
                                               (`user_id`, `last_accessed_at`),
                                           CONSTRAINT `fk_workspace_recent_access_workspace`
                                               FOREIGN KEY (`workspace_id`)
                                                   REFERENCES `workspace` (`workspace_id`)
                                                   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;