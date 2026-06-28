package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.ReportConst;
import com.dto.attachment.AttachmentVO;
import com.dto.report.ReportDetailVO;
import com.dto.report.ReportListItemVO;
import com.dto.report.ReportReviewReq;
import com.dto.report.ReportSubmitReq;
import com.entity.InternReport;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.InternReportMapper;
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
 * 实习报告：学生提交（可附图）→ 教师评分/驳回。
 * 报告类型：MID_TERM（中期报告）、FINAL（终期报告）
 */
@Service
@RequiredArgsConstructor
public class InternReportService {

    private final InternReportMapper reportMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final SysUserMapper sysUserMapper;
    private final AttachmentService attachmentService;

    // ========== 学生 ==========

    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, ReportSubmitReq req) {
        Student student = requireStudentByUserId(userId);

        // 检查是否已提交同类型报告
        long exists = reportMapper.selectCount(new LambdaQueryWrapper<InternReport>()
                .eq(InternReport::getStudentId, student.getId())
                .eq(InternReport::getReportType, req.getReportType()));
        if (exists > 0) {
            throw BizException.validate("该类型报告已提交，不可重复提交");
        }

        InternReport r = new InternReport();
        r.setStudentId(student.getId());
        r.setReportType(req.getReportType());
        r.setTitle(req.getTitle());
        r.setContent(req.getContent());
        r.setStatus(ReportConst.STATUS_SUBMITTED);
        r.setSubmitTime(LocalDateTime.now());
        reportMapper.insert(r);

        attachmentService.bindBiz(userId, ReportConst.BIZ_REPORT, r.getId(), req.getAttachmentIds());

        return r.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, ReportSubmitReq req) {
        Student student = requireStudentByUserId(userId);
        InternReport r = requireReport(id);
        if (!Objects.equals(r.getStudentId(), student.getId())) {
            throw BizException.forbidden("无权操作他人报告");
        }
        if (!ReportConst.STATUS_SUBMITTED.equals(r.getStatus())) {
            throw BizException.validate("教师已评分/驳回，不可修改");
        }
        r.setTitle(req.getTitle());
        r.setContent(req.getContent());
        reportMapper.updateById(r);
        attachmentService.bindBiz(userId, ReportConst.BIZ_REPORT, r.getId(), req.getAttachmentIds());
    }

    public List<ReportListItemVO> myReports(Long userId) {
        Student student = requireStudentByUserId(userId);
        List<InternReport> list = reportMapper.selectList(new LambdaQueryWrapper<InternReport>()
                .eq(InternReport::getStudentId, student.getId())
                .orderByDesc(InternReport::getSubmitTime));
        return enrichListItems(list);
    }

    // ========== 教师 ==========

    public List<ReportListItemVO> teacherPending(Long userId) {
        Set<Long> studentIds = teacherStudentIds(userId);
        if (studentIds.isEmpty()) return new ArrayList<>();
        List<InternReport> list = reportMapper.selectList(new LambdaQueryWrapper<InternReport>()
                .in(InternReport::getStudentId, studentIds)
                .eq(InternReport::getStatus, ReportConst.STATUS_SUBMITTED)
                .orderByDesc(InternReport::getSubmitTime));
        return enrichListItems(list);
    }

    public List<ReportListItemVO> teacherAll(Long userId) {
        Set<Long> studentIds = teacherStudentIds(userId);
        if (studentIds.isEmpty()) return new ArrayList<>();
        List<InternReport> list = reportMapper.selectList(new LambdaQueryWrapper<InternReport>()
                .in(InternReport::getStudentId, studentIds)
                .orderByDesc(InternReport::getSubmitTime));
        return enrichListItems(list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void teacherReview(Long userId, Long id, ReportReviewReq req) {
        if (!ReportConst.RESULT_REVIEWED.equals(req.getResult())
                && !ReportConst.RESULT_REJECTED.equals(req.getResult())) {
            throw BizException.validate("非法的评分结果：" + req.getResult());
        }
        if (ReportConst.RESULT_REVIEWED.equals(req.getResult()) && req.getScore() == null) {
            throw BizException.validate("通过评分必须填写 1-5 分");
        }
        if (ReportConst.RESULT_REJECTED.equals(req.getResult()) && StrUtil.isBlank(req.getComment())) {
            throw BizException.validate("驳回必须填写评语");
        }

        InternReport r = requireReport(id);
        if (!ReportConst.STATUS_SUBMITTED.equals(r.getStatus())) {
            throw BizException.validate("该报告已处理");
        }
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (teacher == null) throw BizException.forbidden("非教师角色");
        Student s = studentMapper.selectById(r.getStudentId());
        if (s == null || !Objects.equals(s.getTeacherId(), teacher.getId())) {
            throw BizException.forbidden("该报告学生不在您的分管范围");
        }

        r.setStatus(req.getResult());
        r.setTeacherScore(ReportConst.RESULT_REVIEWED.equals(req.getResult()) ? req.getScore() : null);
        r.setTeacherComment(req.getComment());
        r.setTeacherReviewTime(LocalDateTime.now());
        r.setTeacherReviewUserId(userId);
        reportMapper.updateById(r);
    }

    // ========== 详情 ==========

    public ReportDetailVO detail(Long userId, String roleCode, Long id) {
        InternReport r = requireReport(id);
        checkDetailAccess(r, userId, roleCode);
        return buildDetail(r);
    }

    // ========== 内部方法 ==========

    private Student requireStudentByUserId(Long userId) {
        Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId).last("LIMIT 1"));
        if (s == null) {
            throw BizException.validate("未找到学生档案，请联系管理员补全");
        }
        return s;
    }

    private InternReport requireReport(Long id) {
        InternReport r = reportMapper.selectById(id);
        if (r == null) throw BizException.notFound("报告不存在：id=" + id);
        return r;
    }

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

    private List<ReportListItemVO> enrichListItems(List<InternReport> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<Long> studentIds = list.stream().map(InternReport::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, x -> x));
        List<Long> userIds = studentMap.values().stream().map(Student::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<ReportListItemVO> result = new ArrayList<>(list.size());
        for (InternReport r : list) {
            ReportListItemVO vo = new ReportListItemVO();
            vo.setId(r.getId());
            vo.setStudentId(r.getStudentId());
            vo.setStudentNo(studentMap.get(r.getStudentId()) == null ? null : studentMap.get(r.getStudentId()).getStudentNo());
            vo.setReportType(r.getReportType());
            vo.setTitle(r.getTitle());
            vo.setAttachmentCount((int) attachmentService.countByBiz(ReportConst.BIZ_REPORT, r.getId()));
            vo.setStatus(r.getStatus());
            vo.setTeacherScore(r.getTeacherScore());
            vo.setSubmitTime(r.getSubmitTime());
            vo.setTeacherReviewTime(r.getTeacherReviewTime());
            vo.setCreateTime(r.getCreateTime());
            Student s = studentMap.get(r.getStudentId());
            if (s != null) {
                SysUser u = userMap.get(s.getUserId());
                if (u != null) vo.setStudentName(u.getRealName());
            }
            result.add(vo);
        }
        return result;
    }

    private ReportDetailVO buildDetail(InternReport r) {
        Student student = studentMapper.selectById(r.getStudentId());
        SysUser user = student == null ? null : sysUserMapper.selectById(student.getUserId());
        SysUser reviewer = r.getTeacherReviewUserId() == null ? null
                : sysUserMapper.selectById(r.getTeacherReviewUserId());

        ReportDetailVO vo = new ReportDetailVO();
        vo.setId(r.getId());
        vo.setStudentId(r.getStudentId());
        vo.setStudentName(user == null ? null : user.getRealName());
        vo.setStudentNo(student == null ? null : student.getStudentNo());
        vo.setReportType(r.getReportType());
        vo.setTitle(r.getTitle());
        vo.setContent(r.getContent());
        vo.setStatus(r.getStatus());
        vo.setTeacherScore(r.getTeacherScore());
        vo.setTeacherComment(r.getTeacherComment());
        vo.setSubmitTime(r.getSubmitTime());
        vo.setTeacherReviewTime(r.getTeacherReviewTime());
        vo.setTeacherReviewUserName(reviewer == null ? null : reviewer.getRealName());
        vo.setCreateTime(r.getCreateTime());

        List<AttachmentVO> attachments = attachmentService.listByBiz(ReportConst.BIZ_REPORT, r.getId());
        vo.setAttachments(attachments);
        return vo;
    }

    private void checkDetailAccess(InternReport r, Long userId, String roleCode) {
        if ("supervisor".equals(roleCode)) return;
        Student s = studentMapper.selectById(r.getStudentId());
        if ("student".equals(roleCode)) {
            Student own = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getUserId, userId).last("LIMIT 1"));
            if (own != null && Objects.equals(own.getId(), r.getStudentId())) return;
        }
        if ("teacher".equals(roleCode)) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                    .eq(Teacher::getUserId, userId).last("LIMIT 1"));
            if (t != null && s != null && Objects.equals(s.getTeacherId(), t.getId())) return;
        }
        throw BizException.forbidden("无权查看该报告");
    }
}
