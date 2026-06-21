package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.common.BizException;
import com.common.RoleEnum;
import com.dto.admin.AdminStudentSaveReq;
import com.dto.admin.AdminStudentVO;
import com.entity.Company;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.mapper.CompanyMapper;
import com.mapper.MentorMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 监管者维护学生档案（账号 + 档案 + 绑定）。
 */
@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherMapper teacherMapper;
    private final MentorMapper mentorMapper;
    private final CompanyMapper companyMapper;

    /** 默认密码（明文，与种子数据一致） */
    private static final String DEFAULT_PASSWORD = "123456";

    public IPage<AdminStudentVO> page(String keyword, String internStatus, Integer status,
                                      long pageNo, long size) {
        final boolean hasKw = StrUtil.isNotBlank(keyword);
        final boolean hasStatus = status != null;

        // 1. 按 keyword + status 在 sys_user 圈出 student 角色的 userIds（可能为 null=不过滤）
        final List<Long> matchedUserIds;
        if (hasKw || hasStatus) {
            LambdaQueryWrapper<SysUser> uq = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, RoleEnum.STUDENT.getCode());
            if (hasKw) {
                final String kw = keyword;
                uq.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
            }
            if (hasStatus) {
                uq.eq(SysUser::getStatus, status);
            }
            matchedUserIds = sysUserMapper.selectList(uq).stream()
                    .map(SysUser::getId).collect(Collectors.toList());
        } else {
            matchedUserIds = null;
        }

        // 2. studentNo 命中的 student ids（keyword 分支）
        final List<Long> studentNoMatchedIds;
        if (hasKw) {
            studentNoMatchedIds = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .like(Student::getStudentNo, keyword))
                    .stream().map(Student::getId).collect(Collectors.toList());
        } else {
            studentNoMatchedIds = null;
        }

        // 3. 仅 status 过滤且 user 无命中、又无 keyword 走 studentNo → 直接空
        if (hasStatus && !hasKw && (matchedUserIds == null || matchedUserIds.isEmpty())) {
            return new Page<AdminStudentVO>(pageNo, size).setRecords(new ArrayList<>());
        }

        // 4. 组装 student 查询
        LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<Student>()
                .eq(StrUtil.isNotBlank(internStatus), Student::getInternStatus, internStatus)
                .orderByDesc(Student::getCreateTime);

        boolean byUser = matchedUserIds != null;
        boolean byStudentNo = studentNoMatchedIds != null && !studentNoMatchedIds.isEmpty();
        if (byUser && byStudentNo) {
            qw.and(w -> w.in(Student::getUserId, matchedUserIds).or().in(Student::getId, studentNoMatchedIds));
        } else if (byUser) {
            if (matchedUserIds.isEmpty()) {
                return new Page<AdminStudentVO>(pageNo, size).setRecords(new ArrayList<>());
            }
            qw.in(Student::getUserId, matchedUserIds);
        } else if (byStudentNo) {
            qw.in(Student::getId, studentNoMatchedIds);
        }

        Page<Student> page = new Page<>(pageNo, size);
        IPage<Student> studentPage = studentMapper.selectPage(page, qw);

        List<AdminStudentVO> vos = toVOs(studentPage.getRecords());
        Page<AdminStudentVO> result = new Page<>(pageNo, size, studentPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    public AdminStudentVO detail(Long id) {
        Student s = studentMapper.selectById(id);
        if (s == null) {
            throw BizException.notFound("学生档案不存在：id=" + id);
        }
        return toVOs(List.of(s)).get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminStudentSaveReq req) {
        // 唯一性校验
        requireUniqueUsername(req.getUsername(), null);
        requireUniqueStudentNo(req.getStudentNo(), null);

        // 建 sys_user 账号
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(StrUtil.isBlank(req.getPassword()) ? DEFAULT_PASSWORD : req.getPassword());
        user.setRealName(req.getRealName());
        user.setRole(RoleEnum.STUDENT.getCode());
        user.setPhone(req.getPhone());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        sysUserMapper.insert(user);

        // 校验绑定目标
        validateBindings(req.getTeacherId(), req.getCompanyId(), req.getMentorId());

        // 建档案
        Student s = new Student();
        s.setUserId(user.getId());
        copyProfile(s, req);
        studentMapper.insert(s);
        return s.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminStudentSaveReq req) {
        Student s = studentMapper.selectById(id);
        if (s == null) {
            throw BizException.notFound("学生档案不存在：id=" + id);
        }
        SysUser user = sysUserMapper.selectById(s.getUserId());
        if (user == null) {
            throw BizException.notFound("关联账号不存在");
        }

        requireUniqueUsername(req.getUsername(), s.getUserId());
        requireUniqueStudentNo(req.getStudentNo(), id);
        validateBindings(req.getTeacherId(), req.getCompanyId(), req.getMentorId());

        // 更新账号（不允许改 role）
        user.setUsername(req.getUsername());
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (StrUtil.isNotBlank(req.getPassword())) user.setPassword(req.getPassword());
        sysUserMapper.updateById(user);

        // 更新档案
        copyProfile(s, req);
        studentMapper.updateById(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Student s = studentMapper.selectById(id);
        if (s == null) return;
        studentMapper.deleteById(id);
        if (s.getUserId() != null) {
            sysUserMapper.deleteById(s.getUserId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void bind(Long studentId, Long teacherId, Long mentorId, Long companyId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) {
            throw BizException.notFound("学生档案不存在：id=" + studentId);
        }
        // 0 = 解绑（置 null），非空非 0 = 校验存在并绑定，null = 不改动
        Long tId = normalize(teacherId);
        Long mId = normalize(mentorId);
        Long cId = normalize(companyId);
        validateBindings(tId, cId, mId);
        if (teacherId != null) s.setTeacherId(tId);
        if (mentorId != null) s.setMentorId(mId);
        if (companyId != null) s.setCompanyId(cId);
        studentMapper.updateById(s);
    }

    /** 0 -> null（解绑），其它原样返回 */
    private Long normalize(Long v) {
        return (v != null && v == 0L) ? null : v;
    }

    // ---------- 内部 ----------

    private void copyProfile(Student s, AdminStudentSaveReq req) {
        s.setStudentNo(req.getStudentNo());
        s.setClassName(req.getClassName());
        s.setMajor(req.getMajor());
        s.setGrade(req.getGrade());
        s.setIdCard(req.getIdCard());
        s.setGender(req.getGender());
        s.setParentPhone(req.getParentPhone());
        s.setInternStart(req.getInternStart());
        s.setInternEnd(req.getInternEnd());
        s.setInternStatus(StrUtil.isBlank(req.getInternStatus()) ? "ACTIVE" : req.getInternStatus());
        s.setTeacherId(req.getTeacherId());
        s.setCompanyId(req.getCompanyId());
        s.setMentorId(req.getMentorId());
    }

    private void requireUniqueUsername(String username, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeUserId != null) qw.ne(SysUser::getId, excludeUserId);
        if (sysUserMapper.selectCount(qw) > 0) {
            throw BizException.validate("登录账号已存在：" + username);
        }
    }

    private void requireUniqueStudentNo(String studentNo, Long excludeId) {
        LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo);
        if (excludeId != null) qw.ne(Student::getId, excludeId);
        if (studentMapper.selectCount(qw) > 0) {
            throw BizException.validate("学号已存在：" + studentNo);
        }
    }

    private void validateBindings(Long teacherId, Long companyId, Long mentorId) {
        if (teacherId != null && teacherMapper.selectById(teacherId) == null) {
            throw BizException.validate("指导教师不存在：id=" + teacherId);
        }
        if (companyId != null && companyMapper.selectById(companyId) == null) {
            throw BizException.validate("实习企业不存在：id=" + companyId);
        }
        if (mentorId != null) {
            Mentor m = mentorMapper.selectById(mentorId);
            if (m == null) {
                throw BizException.validate("企业指导不存在：id=" + mentorId);
            }
            // mentor 与 company 一致性：若同时指定 company，应一致
            if (companyId != null && !Objects.equals(m.getCompanyId(), companyId)) {
                throw BizException.validate("企业指导与实习企业不一致");
            }
        }
    }

    private List<AdminStudentVO> toVOs(List<Student> students) {
        if (students == null || students.isEmpty()) return new ArrayList<>();
        // 批量关联名
        List<Long> userIds = students.stream().map(Student::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Long> teacherIds = students.stream().map(Student::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Teacher> teacherMap = teacherIds.isEmpty() ? new HashMap<>()
                : teacherMapper.selectByIds(teacherIds).stream().collect(Collectors.toMap(Teacher::getId, t -> t));
        List<Long> teacherUserIds = teacherMap.values().stream().map(Teacher::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> teacherUserMap = teacherUserIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(teacherUserIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Long> mentorIds = students.stream().map(Student::getMentorId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Mentor> mentorMap = mentorIds.isEmpty() ? new HashMap<>()
                : mentorMapper.selectByIds(mentorIds).stream().collect(Collectors.toMap(Mentor::getId, m -> m));
        List<Long> mentorUserIds = mentorMap.values().stream().map(Mentor::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> mentorUserMap = mentorUserIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(mentorUserIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Long> companyIds = students.stream().map(Student::getCompanyId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? new HashMap<>()
                : companyMapper.selectByIds(companyIds).stream().collect(Collectors.toMap(Company::getId, c -> c));

        List<AdminStudentVO> result = new ArrayList<>(students.size());
        for (Student s : students) {
            SysUser u = userMap.get(s.getUserId());
            AdminStudentVO vo = new AdminStudentVO();
            vo.setId(s.getId());
            vo.setUserId(s.getUserId());
            vo.setUsername(u == null ? null : u.getUsername());
            vo.setRealName(u == null ? null : u.getRealName());
            vo.setPhone(u == null ? null : u.getPhone());
            vo.setStatus(u == null ? null : u.getStatus());
            vo.setStudentNo(s.getStudentNo());
            vo.setClassName(s.getClassName());
            vo.setMajor(s.getMajor());
            vo.setGrade(s.getGrade());
            vo.setIdCard(s.getIdCard());
            vo.setGender(s.getGender());
            vo.setParentPhone(s.getParentPhone());
            vo.setInternStart(s.getInternStart());
            vo.setInternEnd(s.getInternEnd());
            vo.setInternStatus(s.getInternStatus());
            vo.setTeacherId(s.getTeacherId());
            vo.setCompanyId(s.getCompanyId());
            vo.setMentorId(s.getMentorId());
            Teacher t = teacherMap.get(s.getTeacherId());
            if (t != null) {
                SysUser tu = teacherUserMap.get(t.getUserId());
                vo.setTeacherName(tu == null ? null : tu.getRealName());
            }
            Mentor m = mentorMap.get(s.getMentorId());
            if (m != null) {
                SysUser mu = mentorUserMap.get(m.getUserId());
                vo.setMentorName(mu == null ? null : mu.getRealName());
            }
            Company c = companyMap.get(s.getCompanyId());
            if (c != null) vo.setCompanyName(c.getName());
            vo.setCreateTime(s.getCreateTime());
            result.add(vo);
        }
        return result;
    }
}
