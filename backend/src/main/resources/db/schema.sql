-- ============================================================
-- 实习管理 AI 智能体系统 — MVP Schema (MySQL 8.0+)
-- 字符集：utf8mb4 / 排序规则：utf8mb4_unicode_ci
-- 主键策略：BIGINT（雪花 ID，由 MyBatis-Plus ASSIGN_ID 生成）
-- 软删除：每张表 deleted TINYINT(1) DEFAULT 0
-- 时间字段：create_time / update_time，由 MP 自动填充
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai_admin`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ai_admin`;

-- 为简化迭代，本脚本采用「先 DROP 再 CREATE」幂等策略，仅供 MVP 阶段使用
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `ai_call_log`;
DROP TABLE IF EXISTS `warning`;
DROP TABLE IF EXISTS `attachment`;
DROP TABLE IF EXISTS `approval_flow`;
DROP TABLE IF EXISTS `transfer_apply`;
DROP TABLE IF EXISTS `intern_weekly`;
DROP TABLE IF EXISTS `intern_log`;
DROP TABLE IF EXISTS `leave_apply`;
DROP TABLE IF EXISTS `mentor`;
DROP TABLE IF EXISTS `teacher`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `company`;
DROP TABLE IF EXISTS `sys_user`;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 系统用户表（统一登录入口，4 类角色）
-- ============================================================
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL              COMMENT '主键（雪花ID）',
    `username`        VARCHAR(64)  NOT NULL              COMMENT '登录账号（学号/工号/手机号）',
    `password`        VARCHAR(128) NOT NULL              COMMENT 'BCrypt 加密密码',
    `real_name`       VARCHAR(64)  NOT NULL              COMMENT '真实姓名',
    `role`            VARCHAR(16)  NOT NULL              COMMENT 'supervisor/teacher/student/mentor（小写，对齐 RoleEnum.code）',
    `phone`           VARCHAR(20)  DEFAULT NULL          COMMENT '手机号',
    `avatar`          VARCHAR(255) DEFAULT NULL          COMMENT '头像 URL',
    `status`          TINYINT(1)   NOT NULL DEFAULT 1    COMMENT '状态：1启用 0禁用',
    `last_login_time` DATETIME     DEFAULT NULL          COMMENT '最近登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `deleted`),
    KEY `idx_role` (`role`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '系统用户表';

-- ============================================================
-- 2. 企业档案表（含禁入清单标记）
-- ============================================================
CREATE TABLE `company` (
    `id`               BIGINT       NOT NULL,
    `name`             VARCHAR(128) NOT NULL              COMMENT '企业名称',
    `social_code`      VARCHAR(32)  DEFAULT NULL          COMMENT '统一社会信用代码',
    `address`          VARCHAR(255) DEFAULT NULL          COMMENT '地址',
    `industry`         VARCHAR(64)  DEFAULT NULL          COMMENT '所属行业',
    `contact_person`   VARCHAR(64)  DEFAULT NULL          COMMENT '联系人',
    `contact_phone`    VARCHAR(20)  DEFAULT NULL          COMMENT '联系电话',
    `is_blacklist`     TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '是否禁入清单：1是 0否',
    `blacklist_reason` VARCHAR(255) DEFAULT NULL          COMMENT '禁入原因',
    `remark`           VARCHAR(500) DEFAULT NULL,
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_social_code` (`social_code`, `deleted`),
    KEY `idx_blacklist` (`is_blacklist`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '企业档案表';

-- ============================================================
-- 3. 学生档案表
-- ============================================================
CREATE TABLE `student` (
    `id`            BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL              COMMENT '关联 sys_user.id',
    `student_no`    VARCHAR(32)  NOT NULL              COMMENT '学号',
    `class_name`    VARCHAR(64)  DEFAULT NULL          COMMENT '班级',
    `major`         VARCHAR(64)  DEFAULT NULL          COMMENT '专业',
    `grade`         VARCHAR(16)  DEFAULT NULL          COMMENT '年级（如 2023）',
    `id_card`       VARCHAR(32)  DEFAULT NULL          COMMENT '身份证号',
    `gender`        TINYINT      DEFAULT NULL          COMMENT '性别：1男 2女',
    `teacher_id`    BIGINT       DEFAULT NULL          COMMENT '指导教师（teacher.id）',
    `company_id`    BIGINT       DEFAULT NULL          COMMENT '当前实习企业（company.id）',
    `mentor_id`     BIGINT       DEFAULT NULL          COMMENT '当前企业指导（mentor.id）',
    `intern_start`  DATE         DEFAULT NULL          COMMENT '实习开始日期',
    `intern_end`    DATE         DEFAULT NULL          COMMENT '实习结束日期',
    `intern_status` VARCHAR(16)  DEFAULT 'ACTIVE'      COMMENT 'ACTIVE/SUSPEND/FINISHED/QUIT',
    `parent_phone`  VARCHAR(20)  DEFAULT NULL          COMMENT '家长联系电话',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`, `deleted`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`),
    KEY `idx_teacher` (`teacher_id`),
    KEY `idx_company` (`company_id`),
    KEY `idx_status` (`intern_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '学生档案表';

-- ============================================================
-- 4. 教师档案表
-- ============================================================
CREATE TABLE `teacher` (
    `id`           BIGINT       NOT NULL,
    `user_id`      BIGINT       NOT NULL                COMMENT '关联 sys_user.id',
    `teacher_no`   VARCHAR(32)  NOT NULL                COMMENT '工号',
    `department`   VARCHAR(64)  DEFAULT NULL            COMMENT '所属系/部门',
    `title`        VARCHAR(32)  DEFAULT NULL            COMMENT '职称',
    `office_phone` VARCHAR(20)  DEFAULT NULL,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_no` (`teacher_no`, `deleted`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '教师档案表';

-- ============================================================
-- 5. 企业指导人员档案表
-- ============================================================
CREATE TABLE `mentor` (
    `id`          BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL                  COMMENT '关联 sys_user.id',
    `company_id`  BIGINT       NOT NULL                  COMMENT '所属企业',
    `position`    VARCHAR(64)  DEFAULT NULL              COMMENT '岗位',
    `dept`        VARCHAR(64)  DEFAULT NULL              COMMENT '所在部门',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`),
    KEY `idx_company` (`company_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '企业指导人员档案表';

-- ============================================================
-- 6. 请假申请表
-- ============================================================
CREATE TABLE `leave_apply` (
    `id`             BIGINT       NOT NULL,
    `student_id`     BIGINT       NOT NULL              COMMENT '学生 ID',
    `leave_type`     VARCHAR(16)  NOT NULL              COMMENT '请假类型：SICK/PERSONAL/OTHER',
    `start_time`     DATETIME     NOT NULL              COMMENT '开始时间',
    `end_time`       DATETIME     NOT NULL              COMMENT '结束时间',
    `duration_days`  DECIMAL(8,2) NOT NULL              COMMENT '请假天数（含小数）',
    `reason`         VARCHAR(1000) NOT NULL             COMMENT '请假事由',
    `parent_confirm` TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '家长确认材料是否已上传',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    `current_node`   VARCHAR(16)  DEFAULT NULL          COMMENT '当前审批节点：MENTOR/TEACHER/SUPERVISOR',
    `submit_time`    DATETIME     DEFAULT NULL,
    `finish_time`    DATETIME     DEFAULT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_status` (`status`),
    KEY `idx_current_node` (`current_node`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '请假申请表';

-- ============================================================
-- 7. 实习日志表（日报）
-- ============================================================
CREATE TABLE `intern_log` (
    `id`                    BIGINT       NOT NULL,
    `student_id`            BIGINT       NOT NULL              COMMENT '学生 ID',
    `log_date`              DATE         NOT NULL              COMMENT '日志日期',
    `content`               TEXT         NOT NULL              COMMENT '日志内容',
    `sensitive_hit`         TINYINT(1)   NOT NULL DEFAULT 0    COMMENT 'AI 是否命中敏感词',
    `sensitive_words`       VARCHAR(500) DEFAULT NULL          COMMENT '命中词，逗号分隔',
    `sensitive_marked_html` TEXT         DEFAULT NULL          COMMENT 'AI 高亮后 HTML 片段',
    `mentor_review`         TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '企业指导是否已查阅（CONFIRMED 时置 1，冗余便于查询）',
    `teacher_review`        TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '指导教师是否已查阅',
    `status`                VARCHAR(16)  NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED 待确认 / CONFIRMED 已通过 / REJECTED 已驳回',
    `submit_time`           DATETIME     DEFAULT NULL          COMMENT '学生提交时间',
    `mentor_review_time`    DATETIME     DEFAULT NULL          COMMENT '企业指导确认时间',
    `mentor_review_user_id` BIGINT       DEFAULT NULL          COMMENT '确认人 user_id',
    `mentor_comment`        VARCHAR(500) DEFAULT NULL          COMMENT '企业指导确认意见（驳回必填）',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_date` (`student_id`, `log_date`, `deleted`),
    KEY `idx_log_date` (`log_date`),
    KEY `idx_sensitive` (`sensitive_hit`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '实习日志表';

-- ============================================================
-- 8. 实习周记表
-- ============================================================
CREATE TABLE `intern_weekly` (
    `id`                    BIGINT       NOT NULL,
    `student_id`            BIGINT       NOT NULL              COMMENT '学生 ID',
    `year_week`             VARCHAR(8)   NOT NULL              COMMENT 'ISO 周编号，如 2026-W12',
    `week_start`            DATE         NOT NULL              COMMENT '周一日期',
    `week_end`              DATE         NOT NULL              COMMENT '周日日期',
    `summary`               TEXT         NOT NULL              COMMENT '本周总结',
    `next_plan`             TEXT         DEFAULT NULL          COMMENT '下周计划',
    `sensitive_hit`         TINYINT(1)   NOT NULL DEFAULT 0    COMMENT 'AI 是否命中敏感词',
    `sensitive_words`       VARCHAR(500) DEFAULT NULL,
    `sensitive_marked_html` TEXT         DEFAULT NULL,
    `teacher_score`         TINYINT      DEFAULT NULL          COMMENT '教师评分 1-5',
    `teacher_comment`       VARCHAR(500) DEFAULT NULL,
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_week` (`student_id`, `year_week`, `deleted`),
    KEY `idx_week` (`year_week`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '实习周记表';

-- ============================================================
-- 9. 单位变更申请表
-- ============================================================
CREATE TABLE `transfer_apply` (
    `id`              BIGINT       NOT NULL,
    `student_id`      BIGINT       NOT NULL              COMMENT '学生 ID',
    `from_company_id` BIGINT       NOT NULL              COMMENT '原单位',
    `to_company_id`   BIGINT       DEFAULT NULL          COMMENT '新单位（系统内已登记则非空）',
    `to_company_name` VARCHAR(128) NOT NULL              COMMENT '新单位名称（外部企业冗余存）',
    `reason`          VARCHAR(1000) NOT NULL             COMMENT '变更原因',
    `expected_start`  DATE         DEFAULT NULL          COMMENT '预计入职日期',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    `current_node`    VARCHAR(16)  DEFAULT NULL          COMMENT '当前节点：MENTOR/TEACHER/SUPERVISOR',
    `submit_time`     DATETIME     DEFAULT NULL,
    `finish_time`     DATETIME     DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_status` (`status`),
    KEY `idx_current_node` (`current_node`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '单位变更申请表';

-- ============================================================
-- 10. 审批流水表（leave_apply / transfer_apply 共用）
-- ============================================================
CREATE TABLE `approval_flow` (
    `id`            BIGINT       NOT NULL,
    `biz_type`      VARCHAR(16)  NOT NULL              COMMENT 'LEAVE / TRANSFER',
    `biz_id`        BIGINT       NOT NULL              COMMENT '业务单 ID',
    `node`          VARCHAR(16)  NOT NULL              COMMENT 'MENTOR/TEACHER/SUPERVISOR',
    `node_seq`      INT          NOT NULL              COMMENT '节点序号 1/2/3',
    `approver_id`   BIGINT       DEFAULT NULL          COMMENT '审批人 user_id',
    `approver_name` VARCHAR(64)  DEFAULT NULL          COMMENT '审批人姓名快照',
    `result`        VARCHAR(16)  DEFAULT NULL          COMMENT 'APPROVED/REJECTED；NULL=待审批',
    `comment`       VARCHAR(500) DEFAULT NULL          COMMENT '审批意见（驳回必填）',
    `act_time`      DATETIME     DEFAULT NULL          COMMENT '处理时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_approver` (`approver_id`),
    KEY `idx_node` (`node`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '审批流水表';

-- ----------------------------
-- 11. 附件表（MinIO 元数据）
-- ----------------------------
DROP TABLE IF EXISTS `attachment`;
CREATE TABLE `attachment`
(
    `id`           BIGINT       NOT NULL,
    `biz_type`     VARCHAR(32)  NOT NULL              COMMENT 'LEAVE/LOG/WEEKLY/TRANSFER/USER_AVATAR 等',
    `biz_id`       BIGINT       DEFAULT NULL          COMMENT '业务单 ID（上传时若未生成可为空，提交后回填）',
    `file_name`    VARCHAR(255) NOT NULL              COMMENT '原始文件名',
    `object_key`   VARCHAR(512) NOT NULL              COMMENT 'MinIO object key',
    `bucket`       VARCHAR(64)  NOT NULL DEFAULT 'ai-admin',
    `content_type` VARCHAR(128) DEFAULT NULL          COMMENT 'MIME 类型',
    `size_bytes`   BIGINT       DEFAULT NULL          COMMENT '文件大小（字节）',
    `uploader_id`  BIGINT       NOT NULL              COMMENT '上传人 user_id',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_uploader` (`uploader_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '附件表';

-- ----------------------------
-- 12. 预警表（三色预警事件流）
-- ----------------------------
DROP TABLE IF EXISTS `warning`;
CREATE TABLE `warning`
(
    `id`           BIGINT       NOT NULL,
    `level`        VARCHAR(8)   NOT NULL              COMMENT 'RED/YELLOW/GREEN（GREEN 一般不入库，仅用于聚合）',
    `rule_code`    VARCHAR(32)  NOT NULL              COMMENT 'SENSITIVE_WORD/LEAVE_OVER_3M/COMPANY_BLACKLIST/NO_LOG_3D/TRANSFER_PENDING_3D',
    `rule_desc`    VARCHAR(255) NOT NULL              COMMENT '规则可读描述',
    `student_id`   BIGINT       NOT NULL              COMMENT '关联学生 ID',
    `student_name` VARCHAR(64)  NOT NULL              COMMENT '学生姓名快照',
    `biz_type`     VARCHAR(32)  DEFAULT NULL          COMMENT 'LOG/WEEKLY/LEAVE/TRANSFER/COMPANY',
    `biz_id`       BIGINT       DEFAULT NULL          COMMENT '业务单 ID',
    `detail`       VARCHAR(1000) DEFAULT NULL         COMMENT '命中详情（如敏感词列表 / 企业名 / 缺交天数）',
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REVIEWED/IGNORED',
    `reviewer_id`  BIGINT       DEFAULT NULL          COMMENT '审核人 user_id',
    `review_time`  DATETIME     DEFAULT NULL,
    `review_note`  VARCHAR(500) DEFAULT NULL,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_level_status` (`level`, `status`),
    KEY `idx_student` (`student_id`),
    KEY `idx_rule` (`rule_code`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '三色预警事件表';

-- ----------------------------
-- 13. AI 调用日志表（GLM-4-Air 调用审计）
-- ----------------------------
DROP TABLE IF EXISTS `ai_call_log`;
CREATE TABLE `ai_call_log`
(
    `id`               BIGINT       NOT NULL,
    `scene`            VARCHAR(32)  NOT NULL              COMMENT 'CHAT_QA / SENSITIVE_DETECT',
    `user_id`          BIGINT       DEFAULT NULL          COMMENT '调用人（系统调用为空）',
    `model`            VARCHAR(64)  NOT NULL DEFAULT 'GLM-4-Air-250414',
    `request_payload`  MEDIUMTEXT   DEFAULT NULL          COMMENT '请求体 JSON（messages 等）',
    `response_payload` MEDIUMTEXT   DEFAULT NULL          COMMENT '响应体 JSON',
    `prompt_tokens`    INT          DEFAULT NULL,
    `completion_tokens` INT         DEFAULT NULL,
    `total_tokens`     INT          DEFAULT NULL,
    `cost_ms`          INT          DEFAULT NULL          COMMENT '耗时（毫秒）',
    `success`          TINYINT(1)   NOT NULL DEFAULT 1,
    `error_msg`        VARCHAR(1000) DEFAULT NULL,
    `biz_type`         VARCHAR(32)  DEFAULT NULL          COMMENT '关联业务（LOG/WEEKLY 等，便于追溯敏感词检测来源）',
    `biz_id`           BIGINT       DEFAULT NULL,
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_scene_time` (`scene`, `create_time`),
    KEY `idx_user` (`user_id`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'AI 调用日志表';

SET FOREIGN_KEY_CHECKS = 1;
