USE `ai_admin`;

-- 创建实习报告表
CREATE TABLE IF NOT EXISTS `intern_report` (
    `id`                   BIGINT       NOT NULL,
    `student_id`           BIGINT       NOT NULL              COMMENT '学生 ID',
    `report_type`          VARCHAR(16)  NOT NULL              COMMENT 'MID_TERM/FINAL',
    `title`                VARCHAR(128) NOT NULL              COMMENT '报告标题',
    `content`              TEXT         NOT NULL              COMMENT '报告内容（Markdown）',
    `status`               VARCHAR(16)  NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/REVIEWED/REJECTED',
    `teacher_score`        TINYINT      DEFAULT NULL          COMMENT '教师评分 1-5',
    `teacher_comment`      VARCHAR(500) DEFAULT NULL          COMMENT '教师评语',
    `submit_time`          DATETIME     DEFAULT NULL          COMMENT '学生提交时间',
    `teacher_review_time`  DATETIME     DEFAULT NULL          COMMENT '教师评阅时间',
    `teacher_review_user_id` BIGINT     DEFAULT NULL          COMMENT '评阅教师 user_id',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`              TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_report_type` (`report_type`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '实习报告表';
