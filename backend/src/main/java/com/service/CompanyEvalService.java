package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.CompanyEvalConst;
import com.dto.companyeval.EvalDetailVO;
import com.dto.companyeval.EvalListItemVO;
import com.dto.companyeval.EvalSaveReq;
import com.entity.Company;
import com.entity.CompanyEval;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.CompanyEvalMapper;
import com.mapper.CompanyMapper;
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
 * 企业评价 / 实习鉴定：企业指导（mentor）对所负责学生做四维度评分 + 评语，提交后锁定。
 */
@Service
@RequiredArgsConstructor
public class CompanyEvalService {

    private final CompanyEvalMapper evalMapper;
    private final StudentMapper studentMapper;
    private final MentorMapper mentorMapper;
    private final TeacherMapper teacherMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;

    // ========== mentor ==========

    /** mentor 视角列表：所负责学生，含未评价标记 */
    public List<EvalListItemVO> myStudents(Long userId) {
        Mentor mentor = requireMentorByUserId(userId);
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getMentorId, mentor.getId())
                .orderByAsc(Student::getStudentNo));
        if (students.isEmpty()) return new ArrayList<>();

        List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
        Map<Long, CompanyEval> evalMap = evalMapper.selectList(new LambdaQueryWrapper<CompanyEval>()
                        .in(CompanyEval::getStudentId, studentIds))
                .stream().collect(Collectors.toMap(CompanyEval::getStudentId, e -> e));

        List<Long> userIds = students.stream().map(Student::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<EvalListItemVO> result = new ArrayList<>(students.size());
        for (Student s : students) {
            EvalListItemVO vo = new EvalListItemVO();
            vo.setStudentId(s.getId());
            vo.setStudentNo(s.getStudentNo());
            vo.setClassName(s.getClassName());
            vo.setMajor(s.getMajor());
            vo.setInternStatus(s.getInternStatus());
            SysUser u = userMap.get(s.getUserId());
            vo.setStudentName(u == null ? null : u.getRealName());
            CompanyEval e = evalMap.get(s.getId());
            if (e != null) {
                vo.setId(e.getId());
                vo.setScoreAttendance(e.getScoreAttendance());
                vo.setScoreAbility(e.getScoreAbility());
                vo.setScoreAttitude(e.getScoreAttitude());
                vo.setScoreOverall(e.getScoreOverall());
                vo.setStatus(e.getStatus());
                vo.setSubmitTime(e.getSubmitTime());
                vo.setCreateTime(e.getCreateTime());
                vo.setEvaluated(true);
            } else {
                vo.setEvaluated(false);
            }
            result.add(vo);
        }
        return result;
    }

    /** 新建或更新草稿（同学生已有记录则更新；已提交则禁止改） */
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long userId, EvalSaveReq req) {
        Mentor mentor = requireMentorByUserId(userId);
        Student student = requireStudent(req.getStudentId());
        if (!Objects.equals(student.getMentorId(), mentor.getId())) {
            throw BizException.forbidden("该学生不在您的指导范围");
        }
        validateScores(req);

        CompanyEval e = evalMapper.selectOne(new LambdaQueryWrapper<CompanyEval>()
                .eq(CompanyEval::getStudentId, student.getId()).last("LIMIT 1"));
        if (e == null) {
            e = new CompanyEval();
            e.setStudentId(student.getId());
            e.setMentorId(mentor.getId());
            e.setMentorUserId(userId);
            e.setStatus(CompanyEvalConst.STATUS_DRAFT);
        } else if (CompanyEvalConst.STATUS_SUBMITTED.equals(e.getStatus())) {
            throw BizException.validate("鉴定已提交，不可修改");
        }
        e.setScoreAttendance(req.getScoreAttendance());
        e.setScoreAbility(req.getScoreAbility());
        e.setScoreAttitude(req.getScoreAttitude());
        e.setScoreOverall(req.getScoreOverall());
        e.setComment(req.getComment());
        if (e.getId() == null) {
            evalMapper.insert(e);
        } else {
            evalMapper.updateById(e);
        }
        return e.getId();
    }

    /** 提交锁定 */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, Long id) {
        Mentor mentor = requireMentorByUserId(userId);
        CompanyEval e = requireEval(id);
        if (!Objects.equals(e.getMentorId(), mentor.getId())) {
            throw BizException.forbidden("无权操作该鉴定");
        }
        if (CompanyEvalConst.STATUS_SUBMITTED.equals(e.getStatus())) {
            throw BizException.validate("鉴定已提交");
        }
        if (e.getScoreAttendance() == null || e.getScoreAbility() == null
                || e.getScoreAttitude() == null || e.getScoreOverall() == null) {
            throw BizException.validate("请先完成四维度评分");
        }
        e.setStatus(CompanyEvalConst.STATUS_SUBMITTED);
        e.setSubmitTime(LocalDateTime.now());
        evalMapper.updateById(e);
    }

    // ========== student ==========

    public EvalDetailVO myAppraisal(Long userId) {
        Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId).last("LIMIT 1"));
        if (s == null) throw BizException.validate("未找到学生档案");
        CompanyEval e = evalMapper.selectOne(new LambdaQueryWrapper<CompanyEval>()
                .eq(CompanyEval::getStudentId, s.getId()).last("LIMIT 1"));
        if (e == null) return null;
        return buildDetail(e);
    }

    // ========== 通用详情 ==========

    public EvalDetailVO detail(Long userId, String roleCode, Long id) {
        CompanyEval e = requireEval(id);
        checkDetailAccess(e, userId, roleCode);
        return buildDetail(e);
    }

    // ========== teacher / supervisor 列表 ==========

    public List<EvalListItemVO> teacherAll(Long userId) {
        Set<Long> studentIds = teacherStudentIds(userId);
        return listByStudentIds(studentIds);
    }

    public List<EvalListItemVO> supervisorAll() {
        return listByStudentIds(null);
    }

    private List<EvalListItemVO> listByStudentIds(Set<Long> studentIds) {
        LambdaQueryWrapper<CompanyEval> qw = new LambdaQueryWrapper<CompanyEval>()
                .eq(CompanyEval::getStatus, CompanyEvalConst.STATUS_SUBMITTED)
                .orderByDesc(CompanyEval::getSubmitTime);
        if (studentIds != null) {
            if (studentIds.isEmpty()) return new ArrayList<>();
            qw.in(CompanyEval::getStudentId, studentIds);
        }
        List<CompanyEval> list = evalMapper.selectList(qw);
        if (list.isEmpty()) return new ArrayList<>();

        List<Long> ids = list.stream().map(CompanyEval::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Student::getId, x -> x));
        List<Long> userIds = studentMap.values().stream().map(Student::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<EvalListItemVO> result = new ArrayList<>(list.size());
        for (CompanyEval e : list) {
            EvalListItemVO vo = new EvalListItemVO();
            vo.setId(e.getId());
            vo.setStudentId(e.getStudentId());
            vo.setScoreAttendance(e.getScoreAttendance());
            vo.setScoreAbility(e.getScoreAbility());
            vo.setScoreAttitude(e.getScoreAttitude());
            vo.setScoreOverall(e.getScoreOverall());
            vo.setStatus(e.getStatus());
            vo.setSubmitTime(e.getSubmitTime());
            vo.setCreateTime(e.getCreateTime());
            vo.setEvaluated(true);
            Student s = studentMap.get(e.getStudentId());
            if (s != null) {
                vo.setStudentNo(s.getStudentNo());
                vo.setClassName(s.getClassName());
                vo.setMajor(s.getMajor());
                vo.setInternStatus(s.getInternStatus());
                SysUser u = userMap.get(s.getUserId());
                if (u != null) vo.setStudentName(u.getRealName());
            }
            result.add(vo);
        }
        return result;
    }

    // ========== 内部 ==========

    private void validateScores(EvalSaveReq req) {
        checkScore("出勤与纪律", req.getScoreAttendance());
        checkScore("专业能力", req.getScoreAbility());
        checkScore("工作态度", req.getScoreAttitude());
        checkScore("综合评价", req.getScoreOverall());
        if (StrUtil.isNotBlank(req.getComment()) && req.getComment().length() > 1000) {
            throw BizException.validate("鉴定评语不超过 1000 字");
        }
    }

    private void checkScore(String name, Integer v) {
        if (v == null) throw BizException.validate(name + "评分必填");
        if (v < CompanyEvalConst.SCORE_MIN || v > CompanyEvalConst.SCORE_MAX) {
            throw BizException.validate(name + "评分范围 " + CompanyEvalConst.SCORE_MIN + "-" + CompanyEvalConst.SCORE_MAX);
        }
    }

    private Mentor requireMentorByUserId(Long userId) {
        Mentor m = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                .eq(Mentor::getUserId, userId).last("LIMIT 1"));
        if (m == null) throw BizException.validate("未找到企业指导档案，请联系管理员补全");
        return m;
    }

    private Student requireStudent(Long id) {
        Student s = studentMapper.selectById(id);
        if (s == null) throw BizException.notFound("学生不存在：id=" + id);
        return s;
    }

    private CompanyEval requireEval(Long id) {
        CompanyEval e = evalMapper.selectById(id);
        if (e == null) throw BizException.notFound("鉴定不存在：id=" + id);
        return e;
    }

    private Set<Long> teacherStudentIds(Long userId) {
        Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId).last("LIMIT 1"));
        if (t == null) throw BizException.validate("未找到教师档案");
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                        .eq(Student::getTeacherId, t.getId()))
                .stream().map(Student::getId).collect(Collectors.toSet());
    }

    private EvalDetailVO buildDetail(CompanyEval e) {
        Student s = studentMapper.selectById(e.getStudentId());
        SysUser su = s == null ? null : sysUserMapper.selectById(s.getUserId());
        SysUser mu = e.getMentorUserId() == null ? null : sysUserMapper.selectById(e.getMentorUserId());
        Company c = (s != null && s.getCompanyId() != null) ? companyMapper.selectById(s.getCompanyId()) : null;

        EvalDetailVO vo = new EvalDetailVO();
        vo.setId(e.getId());
        vo.setStudentId(e.getStudentId());
        vo.setStudentName(su == null ? null : su.getRealName());
        vo.setStudentNo(s == null ? null : s.getStudentNo());
        vo.setClassName(s == null ? null : s.getClassName());
        vo.setMajor(s == null ? null : s.getMajor());
        vo.setCompanyName(c == null ? null : c.getName());
        vo.setInternStart(s == null ? null : s.getInternStart());
        vo.setInternEnd(s == null ? null : s.getInternEnd());
        vo.setInternStatus(s == null ? null : s.getInternStatus());
        vo.setMentorName(mu == null ? null : mu.getRealName());
        vo.setScoreAttendance(e.getScoreAttendance());
        vo.setScoreAbility(e.getScoreAbility());
        vo.setScoreAttitude(e.getScoreAttitude());
        vo.setScoreOverall(e.getScoreOverall());
        vo.setComment(e.getComment());
        vo.setStatus(e.getStatus());
        vo.setSubmitTime(e.getSubmitTime());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    private void checkDetailAccess(CompanyEval e, Long userId, String roleCode) {
        if ("supervisor".equals(roleCode)) return;
        Student s = studentMapper.selectById(e.getStudentId());
        if ("mentor".equals(roleCode)) {
            Mentor m = mentorMapper.selectOne(new LambdaQueryWrapper<Mentor>()
                    .eq(Mentor::getUserId, userId).last("LIMIT 1"));
            if (m != null && Objects.equals(e.getMentorId(), m.getId())) return;
        }
        if ("student".equals(roleCode)) {
            Student own = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getUserId, userId).last("LIMIT 1"));
            if (own != null && Objects.equals(own.getId(), e.getStudentId())) return;
        }
        if ("teacher".equals(roleCode)) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                    .eq(Teacher::getUserId, userId).last("LIMIT 1"));
            if (t != null && s != null && Objects.equals(s.getTeacherId(), t.getId())) return;
        }
        throw BizException.forbidden("无权查看该鉴定");
    }
}
