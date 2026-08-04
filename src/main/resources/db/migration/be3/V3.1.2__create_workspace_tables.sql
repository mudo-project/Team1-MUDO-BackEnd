CREATE TABLE `workspace` (
    `workspace_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`   BIGINT       NOT NULL,
    `name`         VARCHAR(100) NOT NULL,
    `created_by`   BIGINT       NOT NULL,
    `deleted_at`   DATETIME(6)  NULL,
    `created_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `active_name`  VARCHAR(100) GENERATED ALWAYS AS (
        CASE WHEN `deleted_at` IS NULL THEN `name` ELSE NULL END
    ) STORED,
    PRIMARY KEY (`workspace_id`),
    UNIQUE KEY `uk_workspace_academy_active_name` (`academy_id`, `active_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `workspace_member` (
    `workspace_id` BIGINT      NOT NULL,
    `user_id`      BIGINT      NOT NULL,
    `created_at`   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`workspace_id`, `user_id`),
    CONSTRAINT `fk_workspace_member_workspace` FOREIGN KEY (`workspace_id`)
        REFERENCES `workspace` (`workspace_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recurring_task_template` (
    `recurring_template_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `workspace_id`          BIGINT       NOT NULL,
    `title`                 VARCHAR(200) NOT NULL,
    `content`               TEXT         NULL,
    `recurrence_type`       VARCHAR(20)  NOT NULL,
    `recurrence_rule`       JSON         NOT NULL,
    `is_active`             TINYINT(1)   NOT NULL DEFAULT 1,
    `created_by`            BIGINT       NOT NULL,
    `created_at`            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`recurring_template_id`),
    CONSTRAINT `fk_recurring_task_template_workspace` FOREIGN KEY (`workspace_id`)
        REFERENCES `workspace` (`workspace_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `task` (
    `task_id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `workspace_id`          BIGINT       NOT NULL,
    `recurring_template_id` BIGINT       NULL,
    `title`                 VARCHAR(200) NOT NULL,
    `content`               TEXT         NULL,
    `status`                VARCHAR(20)  NOT NULL,
    `due_at`                DATETIME(6)  NULL,
    `scheduled_for`         DATETIME(6)  NULL,
    `created_by`            BIGINT       NOT NULL,
    `created_at`            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`task_id`),
    UNIQUE KEY `uk_task_recurring_template_scheduled_for` (`recurring_template_id`, `scheduled_for`),
    CONSTRAINT `fk_task_workspace` FOREIGN KEY (`workspace_id`)
        REFERENCES `workspace` (`workspace_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_task_recurring_template` FOREIGN KEY (`recurring_template_id`)
        REFERENCES `recurring_task_template` (`recurring_template_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `task_comment` (
    `comment_id`   BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`      BIGINT       NOT NULL,
    `content`      TEXT         NOT NULL,
    `is_completed` TINYINT(1)   NOT NULL DEFAULT 0,
    `completed_by` BIGINT       NULL,
    `completed_at` DATETIME(6)  NULL,
    `author_id`    BIGINT       NOT NULL,
    `created_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`comment_id`),
    CONSTRAINT `fk_task_comment_task` FOREIGN KEY (`task_id`)
        REFERENCES `task` (`task_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `task_status_history` (
    `task_status_history_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `task_id`                BIGINT      NOT NULL,
    `previous_status`        VARCHAR(20) NULL,
    `current_status`         VARCHAR(20) NOT NULL,
    `changed_by`             BIGINT      NULL,
    `created_at`             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`task_status_history_id`),
    CONSTRAINT `fk_task_status_history_task` FOREIGN KEY (`task_id`)
        REFERENCES `task` (`task_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `task_comment_mention` (
    `comment_mention_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `comment_id`         BIGINT      NOT NULL,
    `mentioned_user_id`  BIGINT      NOT NULL,
    `created_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`comment_mention_id`),
    UNIQUE KEY `uk_task_comment_mention_comment_user` (`comment_id`, `mentioned_user_id`),
    CONSTRAINT `fk_task_comment_mention_comment` FOREIGN KEY (`comment_id`)
        REFERENCES `task_comment` (`comment_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recurring_task_skip` (
    `recurring_template_id` BIGINT      NOT NULL,
    `scheduled_for`        DATETIME(6) NOT NULL,
    `created_at`           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`recurring_template_id`, `scheduled_for`),
    CONSTRAINT `fk_recurring_task_skip_recurring_template` FOREIGN KEY (`recurring_template_id`)
        REFERENCES `recurring_task_template` (`recurring_template_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
