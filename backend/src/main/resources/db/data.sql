-- =====================================================================
-- 实习管理 AI 智能体系统 - MVP 种子数据 data.sql
-- =====================================================================
-- 用途：开发联调测试数据；统一明文密码 = 123456（MVP 期，上线前切回 BCrypt）
-- 使用：在 schema.sql 执行完后再执行；可重复执行（先 DELETE 再 INSERT）
-- =====================================================================

USE `ai_admin`;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 清理（按依赖逆序）
-- ----------------------------
DELETE FROM `approval_flow` WHERE id BETWEEN 7001 AND 7099;
DELETE FROM `leave_apply`   WHERE id BETWEEN 6001 AND 6099;
DELETE FROM `student`     WHERE id BETWEEN 5001 AND 5099;
DELETE FROM `mentor`      WHERE id BETWEEN 4001 AND 4099;
DELETE FROM `teacher`     WHERE id BETWEEN 3001 AND 3099;
DELETE FROM `company`     WHERE id BETWEEN 2001 AND 2099;
DELETE FROM `sys_user`    WHERE id BETWEEN 1001 AND 1099;

-- ----------------------------
-- 1. sys_user：8 个登录账号（每角色 2 个）
--    密码统一明文 = 123456（MVP）
-- ----------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `phone`, `status`) VALUES
-- 监管者
(1001, 'supervisor01', '123456', '张主任', 'supervisor', '13800000001', 1),
(1002, 'supervisor02', '123456', '李副主任', 'supervisor', '13800000002', 1),
-- 实习指导教师
(1003, 'teacher01',    '123456', '王老师',   'teacher',    '13800000003', 1),
(1004, 'teacher02',    '123456', '陈老师',   'teacher',    '13800000004', 1),
-- 实习学生
(1005, 'student01',    '123456', '林小明',   'student',    '13800000005', 1),
(1006, 'student02',    '123456', '黄小芳',   'student',    '13800000006', 1),
(1007, 'student03',    '123456', '周小华',   'student',    '13800000007', 1),
(1008, 'student04',    '123456', '吴小玲',   'student',    '13800000008', 1);
-- 备注：第 5 位学生（student05）与企业指导（mentor01/02）放在第 9~10 行，下方补齐

INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `phone`, `status`) VALUES
(1009, 'student05',    '123456', '赵小强',   'student',    '13800000009', 1),
(1010, 'mentor01',     '123456', '刘经理',   'mentor',     '13800000010', 1),
(1011, 'mentor02',     '123456', '孙主管',   'mentor',     '13800000011', 1);

-- ----------------------------
-- 2. company：2 家企业（一家正常，一家加入禁入清单做预警测试）
-- ----------------------------
INSERT INTO `company` (`id`, `name`, `social_code`, `address`, `industry`, `contact_person`, `contact_phone`, `is_blacklist`, `blacklist_reason`) VALUES
(2001, '海棠湾喜来登度假酒店', '91460000MA5T0001XA', '海南省海棠湾镇海棠北路', '酒店', '刘经理', '13800000010', 0, NULL),
(2002, '某黑名单旅行社',       '91460000MA5T0002XB', '海南省天涯区',         '旅游', '钱主管', '13800000099', 1, '存在违规用工历史，禁止接收实习生');

-- ----------------------------
-- 3. teacher：2 名指导教师
-- ----------------------------
INSERT INTO `teacher` (`id`, `user_id`, `teacher_no`, `department`, `title`, `office_phone`) VALUES
(3001, 1003, 'T2024001', '旅游管理系', '讲师', '0898-88880001'),
(3002, 1004, 'T2024002', '旅游管理系', '副教授', '0898-88880002');

-- ----------------------------
-- 4. mentor：2 名企业指导（均归属企业 2001）
--    禁入企业 2002 暂不配 mentor，等 student 单位变更时再用
-- ----------------------------
INSERT INTO `mentor` (`id`, `user_id`, `company_id`, `position`, `dept`) VALUES
(4001, 1010, 2001, '前厅部经理', '前厅部'),
(4002, 1011, 2001, '客房部主管', '客房部');

-- ----------------------------
-- 5. student：5 名学生档案
--    绑定关系：
--      student01/02/03 → teacher 3001（王老师），实习企业 2001
--      student04/05    → teacher 3002（陈老师），实习企业 2001
--    intern_start 设为今天 -60 天，便于审批/请假等业务规则验证
-- ----------------------------
INSERT INTO `student` (`id`, `user_id`, `student_no`, `class_name`, `major`, `grade`, `gender`,
                       `teacher_id`, `company_id`, `mentor_id`,
                       `intern_start`, `intern_end`, `intern_status`, `parent_phone`) VALUES
(5001, 1005, '2022030101', '酒店管理2201', '酒店管理', '2022', 1, 3001, 2001, 4001, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 'ACTIVE', '13900000001'),
(5002, 1006, '2022030102', '酒店管理2201', '酒店管理', '2022', 2, 3001, 2001, 4001, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 'ACTIVE', '13900000002'),
(5003, 1007, '2022030103', '酒店管理2201', '酒店管理', '2022', 1, 3001, 2001, 4002, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 'ACTIVE', '13900000003'),
(5004, 1008, '2022040201', '旅游管理2201', '旅游管理', '2022', 2, 3002, 2001, 4002, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 'ACTIVE', '13900000004'),
(5005, 1009, '2022040202', '旅游管理2201', '旅游管理', '2022', 1, 3002, 2001, 4001, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 'ACTIVE', '13900000005');

-- ----------------------------
-- 6. leave_apply：4 条请假样本（覆盖 4 种状态）
--   6001 student01 → 待教师审批（PENDING/TEACHER）         2 天事假
--   6002 student02 → 已驳回（REJECTED，教师驳回）           1 天事假
--   6003 student03 → 已通过（APPROVED，1 天病假，仅教师批） 1 天病假
--   6004 student04 → 待系主任审批（PENDING/SUPERVISOR）     45 天事假（教师已批）
-- ----------------------------
INSERT INTO `leave_apply`
(`id`, `student_id`, `leave_type`, `start_time`, `end_time`, `duration_days`, `reason`, `parent_confirm`, `status`, `current_node`, `submit_time`, `finish_time`)
VALUES
(6001, 5001, 'PERSONAL',
  DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 9 HOUR,
  DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 18 HOUR,
  2.38, '家中有事，需回家处理两天，往返含路途。', 1,
  'PENDING', 'TEACHER', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL),

(6002, 5002, 'PERSONAL',
  DATE_SUB(CURDATE(), INTERVAL 5 DAY) + INTERVAL 9 HOUR,
  DATE_SUB(CURDATE(), INTERVAL 4 DAY) + INTERVAL 18 HOUR,
  1.38, '同学聚会请假一天。', 0,
  'REJECTED', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

(6003, 5003, 'SICK',
  DATE_SUB(CURDATE(), INTERVAL 3 DAY) + INTERVAL 9 HOUR,
  DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 9 HOUR,
  1.00, '感冒发烧，附医院诊断。', 1,
  'APPROVED', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

(6004, 5004, 'PERSONAL',
  DATE_ADD(CURDATE(), INTERVAL 5 DAY) + INTERVAL 9 HOUR,
  DATE_ADD(CURDATE(), INTERVAL 50 DAY) + INTERVAL 18 HOUR,
  45.38, '家中重大变故，需长期请假处理家庭事务，已提供医院/法院相关证明材料。', 1,
  'PENDING', 'SUPERVISOR', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL);

-- ----------------------------
-- 7. approval_flow：每条请假对应的审批流水
-- ----------------------------
INSERT INTO `approval_flow`
(`id`, `biz_type`, `biz_id`, `node`, `node_seq`, `approver_id`, `approver_name`, `result`, `comment`, `act_time`)
VALUES
-- 6001 待教师审批（teacher01=1003 王老师）
(7001, 'LEAVE', 6001, 'TEACHER', 1, 1003, NULL, NULL, NULL, NULL),

-- 6002 已被教师驳回（teacher01=1003）
(7002, 'LEAVE', 6002, 'TEACHER', 1, 1003, '王老师', 'REJECTED', '同学聚会非正当请假事由，不予批准。', DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- 6003 已被教师通过（teacher01=1003）
(7003, 'LEAVE', 6003, 'TEACHER', 1, 1003, '王老师', 'APPROVED', '附件诊断真实，准予休病假一天，注意休息。', DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- 6004 教师已批 + 待系主任审批（teacher02=1004 陈老师）
(7004, 'LEAVE', 6004, 'TEACHER',    1, 1004, '陈老师', 'APPROVED', '事由真实，材料齐全，建议系主任进一步核准。', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7005, 'LEAVE', 6004, 'SUPERVISOR', 2, NULL, NULL, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 测试账号速查表（明文密码统一为 123456）
-- =====================================================================
-- 角色          用户名          姓名     备注
-- supervisor    supervisor01   张主任   监管者主账号
-- supervisor    supervisor02   李副主任
-- teacher       teacher01      王老师   分管 student01/02/03
-- teacher       teacher02      陈老师   分管 student04/05
-- student       student01      林小明   实习企业 喜来登（2001）
-- student       student02      黄小芳   实习企业 喜来登（2001）
-- student       student03      周小华   实习企业 喜来登（2001）
-- student       student04      吴小玲   实习企业 喜来登（2001）
-- student       student05      赵小强   实习企业 喜来登（2001）
-- mentor        mentor01       刘经理   喜来登前厅部
-- mentor        mentor02       孙主管   喜来登客房部
-- =====================================================================
