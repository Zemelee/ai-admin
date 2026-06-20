package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.common.BizException;
import com.common.InternLogConst;
import com.dto.attachment.AttachmentVO;
import com.dto.internlog.InternLogConfirmReq;
import com.dto.internlog.InternLogDetailVO;
import com.dto.internlog.InternLogListItemVO;
import com.dto.internlog.InternLogSubmitReq;
import com.dto.internlog.SensitiveResult;
import com.entity.InternLog;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.InternLogMapper;
import com.mapper.MentorMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实习日志业务：学生每日提交（可附图）→ 企业指导确认/驳回。
 */
@Service
@RequiredArgsConstructor
public class InternLogService {

    private final InternLogMapper internLogMapper;
    private final StudentMapper studentMapper;
    private final MentorMapper mentorMapper;
    private final TeacherMapper teacherMapper;
    private final SysUserMapper sysUserMapper;
    private final LeaveQueryHelper helper;
    private final AttachmentService attachmentService;
    private final SensitiveCheckService sensitiveCheckService;

    // ========== 学生端 ==========

    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, InternLogSubmitReq req) {
        Student student = helper.requireStudentByUserId(userId);
        // 唯一约束兜底：同一天只能提交一条
        Long exists = internLogMapper.selectCount(new LambdaQueryWrapper<InternLog>()
                .eq(InternLog::getStudentId, student.getId())
                .eq(InternLog::getLogDate, req.getLogDate()));
        if (exists != null && exists > 0) {
            throw BizException.validate("当日日志已提交，不可重复提交");
        }

        InternLog log = new InternLog();
        log.setStudentId(student.getId());
        log.setLogDate(req.getLogDate());
        log.setContent(req.getContent());
        log.setSensitiveHit(0);
        log.setMentorReview(0);
        log.setTeacherReview(0);
        log.setStatus(InternLogConst.STATUS_SUBMITTED);
        log.setSubmitTime(LocalDateTime.now());
        internLogMapper.insert(log);

        // 绑定上传时 bizId 为空的图片附件到本日志
        attachmentService.bindBiz(userId, InternLogConst.BIZ_LOG, log.getId(), req.getAttachmentIds());

        // AI 敏感词检测（用工红线），命中回填字段
        SensitiveResult sr = sensitiveCheckService.check(req.getContent(), log.getId(), userId);
        log.setSensitiveHit(sr.isHit() ? 1 : 0);
        log.setSensitiveWords(sr.getWords());
        log.setSensitiveMarkedHtml(sr.getMarkedHtml());
        internLogMapper.updateById(log);
        return log.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, InternLogSubmitReq req) {
        Student student = helper.requireStudentByUserId(userId);
        InternLog log = requireLog(id);
        if (!Objects.equals(log.getStudentId(), student.getId())) {
            throw BizException.forbidden("无权操作他人日志");
        }
        if (!InternLogConst.STATUS_SUBMITTED.equals(log.getStatus())) {
            throw BizException.validate("企业指导已确认，不可修改");
        }
        log.setLogDate(req.getLogDate());
        log.setContent(req.getContent());
        // 内容变更后重新检测敏感词
        SensitiveResult sr = sensitiveCheckService.check(req.getContent(), log.getId(), userId);
        log.setSensitiveHit(sr.isHit() ? 1 : 0);
        log.setSensitiveWords(sr.getWords());
        log.setSensitiveMarkedHtml(sr.getMarkedHtml());
        internLogMapper.updateById(log);
        // 重新绑定附件（覆盖）
        attachmentService.bindBiz(userId, InternLogConst.BIZ_LOG, log.getId(), req.getAttachmentIds());
    }

    public List<InternLogListItemVO> myLogs(Long userId) {
        Student student = helper.requireStudentByUserId(userId);
        List<InternLog> logs = internLogMapper.selectList(new LambdaQueryWrapper<InternLog>()
                .eq(InternLog::getStudentId, student.getId())
                .orderByDesc(InternLog::getLogDate));
        return enrichListItems(logs);
    }

    // ========== 指导教师端 ==========

    /**
     * 教师查看其分管学生的全部日志（含敏感标记），按日期倒序。
     */
    public List<InternLogListItemVO> teacherLogs(Long userId) {
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (teacher == null) {
            throw BizException.validate("未找到指导教师档案，请联系管理员补全");
        }
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getTeacherId, teacher.getId()));
        if (students.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<InternLog> logs = internLogMapper.selectList(new LambdaQueryWrapper<InternLog>()
                .in(InternLog::getStudentId, studentIds)
                .orderByDesc(InternLog::getLogDate));
        List<InternLogListItemVO> result = enrichListItems(logs);
        // 教师查阅后置 teacher_review=1
        if (!logs.isEmpty()) {
            internLogMapper.update(null, new LambdaUpdateWrapper<InternLog>()
                    .in(InternLog::getStudentId, studentIds)
                    .eq(InternLog::getTeacherReview, 0)
                    .set(InternLog::getTeacherReview, 1));
        }
        return result;
    }

    // ========== 企业指导端 ==========

    public List<InternLogListItemVO> mentorPending(Long userId) {
        Mentor mentor = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                .eq(Mentor::getUserId, userId).last("LIMIT 1"));
        if (mentor == null) {
            throw BizException.validate("未找到企业指导档案，请联系管理员补全");
        }
        // 该 mentor 名下所有学生
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getMentorId, mentor.getId()));
        if (students.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<InternLog> logs = internLogMapper.selectList(new LambdaQueryWrapper<InternLog>()
                .in(InternLog::getStudentId, studentIds)
                .eq(InternLog::getStatus, InternLogConst.STATUS_SUBMITTED)
                .orderByDesc(InternLog::getLogDate));
        return enrichListItems(logs);
    }

    @Transactional(rollbackFor = Exception.class)
    public void mentorConfirm(Long userId, Long id, InternLogConfirmReq req) {
        if (!InternLogConst.RESULT_CONFIRMED.equals(req.getResult())
                && !InternLogConst.RESULT_REJECTED.equals(req.getResult())) {
            throw BizException.validate("非法的确认结果：" + req.getResult());
        }
        if (InternLogConst.RESULT_REJECTED.equals(req.getResult()) && StrUtil.isBlank(req.getComment())) {
            throw BizException.validate("驳回必须填写确认意见");
        }

        InternLog log = requireLog(id);
        if (!InternLogConst.STATUS_SUBMITTED.equals(log.getStatus())) {
            throw BizException.validate("该日志已处理，无需重复确认");
        }
        // 校验该日志学生属于当前 mentor
        Mentor mentor = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                .eq(Mentor::getUserId, userId).last("LIMIT 1"));
        if (mentor == null) {
            throw BizException.forbidden("非企业指导角色");
        }
        Student student = studentMapper.selectById(log.getStudentId());
        if (student == null || !Objects.equals(student.getMentorId(), mentor.getId())) {
            throw BizException.forbidden("该日志学生不在您的指导范围内");
        }

        log.setStatus(req.getResult());
        log.setMentorReviewTime(LocalDateTime.now());
        log.setMentorReviewUserId(userId);
        log.setMentorComment(req.getComment());
        if (InternLogConst.RESULT_CONFIRMED.equals(req.getResult())) {
            log.setMentorReview(1);
        }
        internLogMapper.updateById(log);
    }

    // ========== 通用详情 ==========

    public InternLogDetailVO detail(Long userId, String roleCode, Long id) {
        InternLog log = requireLog(id);
        checkDetailAccess(log, userId, roleCode);
        return buildDetail(log);
    }

    // ========== 内部工具 ==========

    private InternLog requireLog(Long id) {
        InternLog log = internLogMapper.selectById(id);
        if (log == null) {
            throw BizException.notFound("实习日志不存在：id=" + id);
        }
        return log;
    }

    private List<InternLogListItemVO> enrichListItems(List<InternLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> studentIds = logs.stream().map(InternLog::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        List<Long> userIds = studentMap.values().stream().map(Student::getUserId)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<InternLogListItemVO> result = new ArrayList<>(logs.size());
        for (InternLog l : logs) {
            InternLogListItemVO vo = new InternLogListItemVO();
            vo.setId(l.getId());
            vo.setStudentId(l.getStudentId());
            vo.setLogDate(l.getLogDate());
            vo.setContentSummary(summary(l.getContent()));
            vo.setAttachmentCount((int) attachmentService.countByBiz(InternLogConst.BIZ_LOG, l.getId()));
            vo.setSensitiveHit(l.getSensitiveHit());
            vo.setStatus(l.getStatus());
            vo.setSubmitTime(l.getSubmitTime());
            vo.setMentorReviewTime(l.getMentorReviewTime());
            vo.setCreateTime(l.getCreateTime());
            Student s = studentMap.get(l.getStudentId());
            if (s != null) {
                vo.setStudentNo(s.getStudentNo());
                SysUser u = userMap.get(s.getUserId());
                if (u != null) vo.setStudentName(u.getRealName());
            }
            result.add(vo);
        }
        return result;
    }

    private InternLogDetailVO buildDetail(InternLog log) {
        Student student = studentMapper.selectById(log.getStudentId());
        SysUser user = student == null ? null : sysUserMapper.selectById(student.getUserId());
        SysUser reviewer = log.getMentorReviewUserId() == null ? null
                : sysUserMapper.selectById(log.getMentorReviewUserId());

        InternLogDetailVO vo = new InternLogDetailVO();
        vo.setId(log.getId());
        vo.setStudentId(log.getStudentId());
        vo.setStudentName(user == null ? null : user.getRealName());
        vo.setStudentNo(student == null ? null : student.getStudentNo());
        vo.setLogDate(log.getLogDate());
        vo.setContent(log.getContent());
        vo.setSensitiveHit(log.getSensitiveHit());
        vo.setSensitiveWords(log.getSensitiveWords());
        vo.setSensitiveMarkedHtml(log.getSensitiveMarkedHtml());
        vo.setStatus(log.getStatus());
        vo.setSubmitTime(log.getSubmitTime());
        vo.setMentorReviewTime(log.getMentorReviewTime());
        vo.setMentorReviewUserName(reviewer == null ? null : reviewer.getRealName());
        vo.setMentorComment(log.getMentorComment());
        vo.setCreateTime(log.getCreateTime());

        List<AttachmentVO> attachments = attachmentService.listByBiz(InternLogConst.BIZ_LOG, log.getId());
        vo.setAttachments(attachments);
        return vo;
    }

    private void checkDetailAccess(InternLog log, Long userId, String roleCode) {
        if ("supervisor".equals(roleCode)) {
            return;
        }
        if ("student".equals(roleCode)) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getUserId, userId).last("LIMIT 1"));
            if (s != null && Objects.equals(s.getId(), log.getStudentId())) {
                return;
            }
        }
        if ("mentor".equals(roleCode)) {
            Mentor m = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                    .eq(Mentor::getUserId, userId).last("LIMIT 1"));
            Student s = studentMapper.selectById(log.getStudentId());
            if (m != null && s != null && Objects.equals(s.getMentorId(), m.getId())) {
                return;
            }
        }
        if ("teacher".equals(roleCode)) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                    .eq(Teacher::getUserId, userId).last("LIMIT 1"));
            Student s = studentMapper.selectById(log.getStudentId());
            if (t != null && s != null && Objects.equals(s.getTeacherId(), t.getId())) {
                return;
            }
        }
        throw BizException.forbidden("无权查看该日志");
    }

    private String summary(String content) {
        if (StrUtil.isBlank(content)) return "";
        String s = content.replaceAll("\\s+", " ").trim();
        return s.length() <= 50 ? s : s.substring(0, 50) + "…";
    }
}
