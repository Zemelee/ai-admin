package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.common.BizException;
import com.common.RoleEnum;
import com.dto.admin.AdminTeacherSaveReq;
import com.dto.admin.AdminTeacherVO;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
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
import java.util.stream.Collectors;

/**
 * supervisor 维护教师账号与档案
 */
@Service
@RequiredArgsConstructor
public class AdminTeacherService {

    private final TeacherMapper teacherMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;

    private static final String DEFAULT_PASSWORD = "123456";

    public IPage<AdminTeacherVO> page(String keyword, Integer status, long pageNo, long size) {
        final boolean hasKw = StrUtil.isNotBlank(keyword);
        final boolean hasStatus = status != null;

        final List<Long> matchedUserIds;
        if (hasKw || hasStatus) {
            LambdaQueryWrapper<SysUser> uq = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, RoleEnum.TEACHER.getCode());
            if (hasKw) {
                final String kw = keyword;
                uq.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
            }
            if (hasStatus) uq.eq(SysUser::getStatus, status);
            matchedUserIds = sysUserMapper.selectList(uq).stream().map(SysUser::getId).collect(Collectors.toList());
        } else {
            matchedUserIds = null;
        }

        final List<Long> byTeacherNo = hasKw ? teacherMapper.selectList(new LambdaQueryWrapper<Teacher>()
                .like(Teacher::getTeacherNo, keyword)).stream().map(Teacher::getId).collect(Collectors.toList())
                : null;

        if (hasStatus && !hasKw && (matchedUserIds == null || matchedUserIds.isEmpty())) {
            return new Page<AdminTeacherVO>(pageNo, size).setRecords(new ArrayList<>());
        }

        LambdaQueryWrapper<Teacher> qw = new LambdaQueryWrapper<Teacher>().orderByDesc(Teacher::getCreateTime);
        boolean byUser = matchedUserIds != null;
        boolean byNo = byTeacherNo != null && !byTeacherNo.isEmpty();
        if (byUser && byNo) {
            qw.and(w -> w.in(Teacher::getUserId, matchedUserIds).or().in(Teacher::getId, byTeacherNo));
        } else if (byUser) {
            if (matchedUserIds.isEmpty()) {
                return new Page<AdminTeacherVO>(pageNo, size).setRecords(new ArrayList<>());
            }
            qw.in(Teacher::getUserId, matchedUserIds);
        } else if (byNo) {
            qw.in(Teacher::getId, byTeacherNo);
        }

        Page<Teacher> page = new Page<>(pageNo, size);
        IPage<Teacher> tp = teacherMapper.selectPage(page, qw);
        List<AdminTeacherVO> vos = toVOs(tp.getRecords());
        Page<AdminTeacherVO> result = new Page<>(pageNo, size, tp.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminTeacherSaveReq req) {
        requireUniqueUsername(req.getUsername(), null);
        requireUniqueTeacherNo(req.getTeacherNo(), null);

        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setPassword(StrUtil.isBlank(req.getPassword()) ? DEFAULT_PASSWORD : req.getPassword());
        u.setRealName(req.getRealName());
        u.setRole(RoleEnum.TEACHER.getCode());
        u.setPhone(req.getPhone());
        u.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        sysUserMapper.insert(u);

        Teacher t = new Teacher();
        t.setUserId(u.getId());
        copyProfile(t, req);
        teacherMapper.insert(t);
        return t.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminTeacherSaveReq req) {
        Teacher t = teacherMapper.selectById(id);
        if (t == null) throw BizException.notFound("教师档案不存在");
        SysUser u = sysUserMapper.selectById(t.getUserId());
        if (u == null) throw BizException.notFound("关联账号不存在");

        requireUniqueUsername(req.getUsername(), u.getId());
        requireUniqueTeacherNo(req.getTeacherNo(), id);

        u.setUsername(req.getUsername());
        u.setRealName(req.getRealName());
        u.setPhone(req.getPhone());
        if (req.getStatus() != null) u.setStatus(req.getStatus());
        if (StrUtil.isNotBlank(req.getPassword())) u.setPassword(req.getPassword());
        sysUserMapper.updateById(u);

        copyProfile(t, req);
        teacherMapper.updateById(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Teacher t = teacherMapper.selectById(id);
        if (t == null) return;
        if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getTeacherId, id)) > 0) {
            throw BizException.validate("该教师仍有分管学生，无法删除");
        }
        teacherMapper.deleteById(id);
        if (t.getUserId() != null) sysUserMapper.deleteById(t.getUserId());
    }

    private void copyProfile(Teacher t, AdminTeacherSaveReq req) {
        t.setTeacherNo(req.getTeacherNo());
        t.setDepartment(req.getDepartment());
        t.setTitle(req.getTitle());
        t.setOfficePhone(req.getOfficePhone());
    }

    private void requireUniqueUsername(String username, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username);
        if (excludeUserId != null) qw.ne(SysUser::getId, excludeUserId);
        if (sysUserMapper.selectCount(qw) > 0) throw BizException.validate("登录账号已存在：" + username);
    }

    private void requireUniqueTeacherNo(String no, Long excludeId) {
        LambdaQueryWrapper<Teacher> qw = new LambdaQueryWrapper<Teacher>().eq(Teacher::getTeacherNo, no);
        if (excludeId != null) qw.ne(Teacher::getId, excludeId);
        if (teacherMapper.selectCount(qw) > 0) throw BizException.validate("工号已存在：" + no);
    }

    private List<AdminTeacherVO> toVOs(List<Teacher> teachers) {
        if (teachers == null || teachers.isEmpty()) return new ArrayList<>();
        List<Long> userIds = teachers.stream().map(Teacher::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        List<AdminTeacherVO> result = new ArrayList<>(teachers.size());
        for (Teacher t : teachers) {
            SysUser u = userMap.get(t.getUserId());
            AdminTeacherVO vo = new AdminTeacherVO();
            vo.setId(t.getId());
            vo.setUserId(t.getUserId());
            vo.setUsername(u == null ? null : u.getUsername());
            vo.setRealName(u == null ? null : u.getRealName());
            vo.setPhone(u == null ? null : u.getPhone());
            vo.setStatus(u == null ? null : u.getStatus());
            vo.setTeacherNo(t.getTeacherNo());
            vo.setDepartment(t.getDepartment());
            vo.setTitle(t.getTitle());
            vo.setOfficePhone(t.getOfficePhone());
            vo.setCreateTime(t.getCreateTime());
            vo.setStudentCount(studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getTeacherId, t.getId())));
            result.add(vo);
        }
        return result;
    }
}
