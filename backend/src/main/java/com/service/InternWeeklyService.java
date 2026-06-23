package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.WeeklyConst;
import com.dto.attachment.AttachmentVO;
import com.dto.internlog.SensitiveResult;
import com.dto.weekly.WeeklyDetailVO;
import com.dto.weekly.WeeklyListItemVO;
import com.dto.weekly.WeeklyReviewReq;
import com.dto.weekly.WeeklySubmitReq;
import com.entity.InternWeekly;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.InternWeeklyMapper;
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
 * 实习周记：学生提交（可附图）→ 教师评分/驳回。
 */
@Service
@RequiredArgsConstructor
public class InternWeeklyService {

    private final InternWeeklyMapper weeklyMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final MentorMapper mentorMapper;
    private final SysUserMapper sysUserMapper;
    private final LeaveQueryHelper helper;
    private final AttachmentService attachmentService;
    private final SensitiveCheckService sensitiveCheckService;

    // ========== 学生 ==========

    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, WeeklySubmitReq req) {
        Student student = helper.requireStudentByUserId(userId);
        long exists = weeklyMapper.selectCount(new LambdaQueryWrapper<InternWeekly>()
                .eq(InternWeekly::getStudentId, student.getId())
                .eq(InternWeekly::getYearWeek, req.getYearWeek()));
        if (exists > 0) {
            throw BizException.validate("本周周记已提交，不可重复提交");
        }

        InternWeekly w = new InternWeekly();
        w.setStudentId(student.getId());
        w.setYearWeek(req.getYearWeek());
        w.setWeekStart(req.getWeekStart());
        w.setWeekEnd(req.getWeekEnd());
        w.setSummary(req.getSummary());
        w.setNextPlan(req.getNextPlan());
        w.setSensitiveHit(0);
        w.setStatus(WeeklyConst.STATUS_SUBMITTED);
        w.setSubmitTime(LocalDateTime.now());
        weeklyMapper.insert(w);

        attachmentService.bindBiz(userId, WeeklyConst.BIZ_WEEKLY, w.getId(), req.getAttachmentIds());

        // AI 敏感词检测（基于 summary + nextPlan）
        String fullText = req.getSummary() + (StrUtil.isBlank(req.getNextPlan()) ? "" : "\n\n下周计划：\n" + req.getNextPlan());
        SensitiveResult sr = sensitiveCheckService.check(fullText, w.getId(), userId);
        w.setSensitiveHit(sr.isHit() ? 1 : 0);
        w.setSensitiveWords(sr.getWords());
        w.setSensitiveMarkedHtml(sr.getMarkedHtml());
        weeklyMapper.updateById(w);
        return w.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, WeeklySubmitReq req) {
        Student student = helper.requireStudentByUserId(userId);
        InternWeekly w = requireWeekly(id);
        if (!Objects.equals(w.getStudentId(), student.getId())) {
            throw BizException.forbidden("无权操作他人周记");
        }
        if (!WeeklyConst.STATUS_SUBMITTED.equals(w.getStatus())) {
            throw BizException.validate("教师已评分/驳回，不可修改");
        }
        w.setYearWeek(req.getYearWeek());
        w.setWeekStart(req.getWeekStart());
        w.setWeekEnd(req.getWeekEnd());
        w.setSummary(req.getSummary());
        w.setNextPlan(req.getNextPlan());
        String fullText = req.getSummary() + (StrUtil.isBlank(req.getNextPlan()) ? "" : "\n\n下周计划：\n" + req.getNextPlan());
        SensitiveResult sr = sensitiveCheckService.check(fullText, w.getId(), userId);
        w.setSensitiveHit(sr.isHit() ? 1 : 0);
        w.setSensitiveWords(sr.getWords());
        w.setSensitiveMarkedHtml(sr.getMarkedHtml());
        weeklyMapper.updateById(w);
        attachmentService.bindBiz(userId, WeeklyConst.BIZ_WEEKLY, w.getId(), req.getAttachmentIds());
    }

    public List<WeeklyListItemVO> myWeeklies(Long userId) {
        Student student = helper.requireStudentByUserId(userId);
        List<InternWeekly> list = weeklyMapper.selectList(new LambdaQueryWrapper<InternWeekly>()
                .eq(InternWeekly::getStudentId, student.getId())
                .orderByDesc(InternWeekly::getWeekStart));
        return enrichListItems(list);
    }

    // ========== 教师 ==========

    public List<WeeklyListItemVO> teacherPending(Long userId) {
        Set<Long> studentIds = teacherStudentIds(userId);
        if (studentIds.isEmpty()) return new ArrayList<>();
        List<InternWeekly> list = weeklyMapper.selectList(new LambdaQueryWrapper<InternWeekly>()
                .in(InternWeekly::getStudentId, studentIds)
                .eq(InternWeekly::getStatus, WeeklyConst.STATUS_SUBMITTED)
                .orderByDesc(InternWeekly::getWeekStart));
        return enrichListItems(list);
    }

    public List<WeeklyListItemVO> teacherAll(Long userId) {
        Set<Long> studentIds = teacherStudentIds(userId);
        if (studentIds.isEmpty()) return new ArrayList<>();
        List<InternWeekly> list = weeklyMapper.selectList(new LambdaQueryWrapper<InternWeekly>()
                .in(InternWeekly::getStudentId, studentIds)
                .orderByDesc(InternWeekly::getWeekStart));
        return enrichListItems(list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void teacherReview(Long userId, Long id, WeeklyReviewReq req) {
        if (!WeeklyConst.RESULT_REVIEWED.equals(req.getResult())
                && !WeeklyConst.RESULT_REJECTED.equals(req.getResult())) {
            throw BizException.validate("非法的评分结果：" + req.getResult());
        }
        if (WeeklyConst.RESULT_REVIEWED.equals(req.getResult()) && req.getScore() == null) {
            throw BizException.validate("通过评分必须填写 1-5 分");
        }
        if (WeeklyConst.RESULT_REJECTED.equals(req.getResult()) && StrUtil.isBlank(req.getComment())) {
            throw BizException.validate("驳回必须填写评语");
        }

        InternWeekly w = requireWeekly(id);
        if (!WeeklyConst.STATUS_SUBMITTED.equals(w.getStatus())) {
            throw BizException.validate("该周记已处理");
        }
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (teacher == null) throw BizException.forbidden("非教师角色");
        Student s = studentMapper.selectById(w.getStudentId());
        if (s == null || !Objects.equals(s.getTeacherId(), teacher.getId())) {
            throw BizException.forbidden("该周记学生不在您的分管范围");
        }

        w.setStatus(req.getResult());
        w.setTeacherScore(WeeklyConst.RESULT_REVIEWED.equals(req.getResult()) ? req.getScore() : null);
        w.setTeacherComment(req.getComment());
        w.setTeacherReviewTime(LocalDateTime.now());
        w.setTeacherReviewUserId(userId);
        weeklyMapper.updateById(w);
    }

    // ========== 详情 ==========

    public WeeklyDetailVO detail(Long userId, String roleCode, Long id) {
        InternWeekly w = requireWeekly(id);
        checkDetailAccess(w, userId, roleCode);
        return buildDetail(w);
    }

    // ========== 内部 ==========

    private Set<Long> teacherStudentIds(Long userId) {
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (teacher == null) {
            throw BizException.validate("未找到教师档案，请联系管理员补全");
        }
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                        .eq(Student::getTeacherId, teacher.getId()))
                .stream().map(Student::getId).collect(Collectors.toSet());
    }

    private InternWeekly requireWeekly(Long id) {
        InternWeekly w = weeklyMapper.selectById(id);
        if (w == null) throw BizException.notFound("周记不存在：id=" + id);
        return w;
    }

    private List<WeeklyListItemVO> enrichListItems(List<InternWeekly> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<Long> studentIds = list.stream().map(InternWeekly::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, x -> x));
        List<Long> userIds = studentMap.values().stream().map(Student::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<WeeklyListItemVO> result = new ArrayList<>(list.size());
        for (InternWeekly w : list) {
            WeeklyListItemVO vo = new WeeklyListItemVO();
            vo.setId(w.getId());
            vo.setStudentId(w.getStudentId());
            vo.setYearWeek(w.getYearWeek());
            vo.setWeekStart(w.getWeekStart());
            vo.setWeekEnd(w.getWeekEnd());
            vo.setSummarySummary(summary(w.getSummary()));
            vo.setAttachmentCount((int) attachmentService.countByBiz(WeeklyConst.BIZ_WEEKLY, w.getId()));
            vo.setSensitiveHit(w.getSensitiveHit());
            vo.setStatus(w.getStatus());
            vo.setTeacherScore(w.getTeacherScore());
            vo.setSubmitTime(w.getSubmitTime());
            vo.setTeacherReviewTime(w.getTeacherReviewTime());
            vo.setCreateTime(w.getCreateTime());
            Student s = studentMap.get(w.getStudentId());
            if (s != null) {
                vo.setStudentNo(s.getStudentNo());
                SysUser u = userMap.get(s.getUserId());
                if (u != null) vo.setStudentName(u.getRealName());
            }
            result.add(vo);
        }
        return result;
    }

    private WeeklyDetailVO buildDetail(InternWeekly w) {
        Student student = studentMapper.selectById(w.getStudentId());
        SysUser user = student == null ? null : sysUserMapper.selectById(student.getUserId());
        SysUser reviewer = w.getTeacherReviewUserId() == null ? null
                : sysUserMapper.selectById(w.getTeacherReviewUserId());

        WeeklyDetailVO vo = new WeeklyDetailVO();
        vo.setId(w.getId());
        vo.setStudentId(w.getStudentId());
        vo.setStudentName(user == null ? null : user.getRealName());
        vo.setStudentNo(student == null ? null : student.getStudentNo());
        vo.setYearWeek(w.getYearWeek());
        vo.setWeekStart(w.getWeekStart());
        vo.setWeekEnd(w.getWeekEnd());
        vo.setSummary(w.getSummary());
        vo.setNextPlan(w.getNextPlan());
        vo.setSensitiveHit(w.getSensitiveHit());
        vo.setSensitiveWords(w.getSensitiveWords());
        vo.setSensitiveMarkedHtml(w.getSensitiveMarkedHtml());
        vo.setStatus(w.getStatus());
        vo.setTeacherScore(w.getTeacherScore());
        vo.setTeacherComment(w.getTeacherComment());
        vo.setSubmitTime(w.getSubmitTime());
        vo.setTeacherReviewTime(w.getTeacherReviewTime());
        vo.setTeacherReviewUserName(reviewer == null ? null : reviewer.getRealName());
        vo.setCreateTime(w.getCreateTime());

        List<AttachmentVO> attachments = attachmentService.listByBiz(WeeklyConst.BIZ_WEEKLY, w.getId());
        vo.setAttachments(attachments);
        return vo;
    }

    private void checkDetailAccess(InternWeekly w, Long userId, String roleCode) {
        if ("supervisor".equals(roleCode)) return;
        Student s = studentMapper.selectById(w.getStudentId());
        if ("student".equals(roleCode)) {
            Student own = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getUserId, userId).last("LIMIT 1"));
            if (own != null && Objects.equals(own.getId(), w.getStudentId())) return;
        }
        if ("teacher".equals(roleCode)) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                    .eq(Teacher::getUserId, userId).last("LIMIT 1"));
            if (t != null && s != null && Objects.equals(s.getTeacherId(), t.getId())) return;
        }
        if ("mentor".equals(roleCode)) {
            Mentor m = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                    .eq(Mentor::getUserId, userId).last("LIMIT 1"));
            if (m != null && s != null && Objects.equals(s.getMentorId(), m.getId())) return;
        }
        throw BizException.forbidden("无权查看该周记");
    }

    private String summary(String content) {
        if (StrUtil.isBlank(content)) return "";
        String s = content.replaceAll("\\s+", " ").trim();
        return s.length() <= 60 ? s : s.substring(0, 60) + "…";
    }
}
