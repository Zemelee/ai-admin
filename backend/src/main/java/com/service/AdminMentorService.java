package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.common.BizException;
import com.common.RoleEnum;
import com.dto.admin.AdminMentorSaveReq;
import com.dto.admin.AdminMentorVO;
import com.entity.Company;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.mapper.CompanyMapper;
import com.mapper.MentorMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * supervisor 维护企业指导（mentor）账号与档案
 */
@Service
@RequiredArgsConstructor
public class AdminMentorService {

    private final MentorMapper mentorMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;
    private final StudentMapper studentMapper;

    private static final String DEFAULT_PASSWORD = "123456";

    public IPage<AdminMentorVO> page(String keyword, Long companyId, Integer status, long pageNo, long size) {
        final boolean hasKw = StrUtil.isNotBlank(keyword);
        final boolean hasStatus = status != null;

        final List<Long> matchedUserIds;
        if (hasKw || hasStatus) {
            LambdaQueryWrapper<SysUser> uq = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, RoleEnum.MENTOR.getCode());
            if (hasKw) {
                final String kw = keyword;
                uq.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
            }
            if (hasStatus) uq.eq(SysUser::getStatus, status);
            matchedUserIds = sysUserMapper.selectList(uq).stream().map(SysUser::getId).collect(Collectors.toList());
        } else {
            matchedUserIds = null;
        }

        if (hasStatus && !hasKw && (matchedUserIds == null || matchedUserIds.isEmpty())) {
            return new Page<AdminMentorVO>(pageNo, size).setRecords(new ArrayList<>());
        }

        LambdaQueryWrapper<Mentor> qw = new LambdaQueryWrapper<Mentor>()
                .eq(companyId != null, Mentor::getCompanyId, companyId)
                .orderByDesc(Mentor::getCreateTime);
        if (matchedUserIds != null) {
            if (matchedUserIds.isEmpty()) {
                return new Page<AdminMentorVO>(pageNo, size).setRecords(new ArrayList<>());
            }
            qw.in(Mentor::getUserId, matchedUserIds);
        }

        Page<Mentor> page = new Page<>(pageNo, size);
        IPage<Mentor> mp = mentorMapper.selectPage(page, qw);
        List<AdminMentorVO> vos = toVOs(mp.getRecords());
        Page<AdminMentorVO> result = new Page<>(pageNo, size, mp.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminMentorSaveReq req) {
        requireUniqueUsername(req.getUsername(), null);
        requireCompany(req.getCompanyId());

        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setPassword(StrUtil.isBlank(req.getPassword()) ? DEFAULT_PASSWORD : req.getPassword());
        u.setRealName(req.getRealName());
        u.setRole(RoleEnum.MENTOR.getCode());
        u.setPhone(req.getPhone());
        u.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        sysUserMapper.insert(u);

        Mentor m = new Mentor();
        m.setUserId(u.getId());
        copyProfile(m, req);
        mentorMapper.insert(m);
        return m.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminMentorSaveReq req) {
        Mentor m = mentorMapper.selectById(id);
        if (m == null) throw BizException.notFound("企业指导档案不存在");
        SysUser u = sysUserMapper.selectById(m.getUserId());
        if (u == null) throw BizException.notFound("关联账号不存在");

        requireUniqueUsername(req.getUsername(), u.getId());
        requireCompany(req.getCompanyId());

        u.setUsername(req.getUsername());
        u.setRealName(req.getRealName());
        u.setPhone(req.getPhone());
        if (req.getStatus() != null) u.setStatus(req.getStatus());
        if (StrUtil.isNotBlank(req.getPassword())) u.setPassword(req.getPassword());
        sysUserMapper.updateById(u);

        copyProfile(m, req);
        mentorMapper.updateById(m);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Mentor m = mentorMapper.selectById(id);
        if (m == null) return;
        if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getMentorId, id)) > 0) {
            throw BizException.validate("该企业指导仍有指导学生，无法删除");
        }
        mentorMapper.deleteById(id);
        if (m.getUserId() != null) sysUserMapper.deleteById(m.getUserId());
    }

    private void copyProfile(Mentor m, AdminMentorSaveReq req) {
        m.setCompanyId(req.getCompanyId());
        m.setPosition(req.getPosition());
        m.setDept(req.getDept());
    }

    private void requireUniqueUsername(String username, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username);
        if (excludeUserId != null) qw.ne(SysUser::getId, excludeUserId);
        if (sysUserMapper.selectCount(qw) > 0) throw BizException.validate("登录账号已存在：" + username);
    }

    private void requireCompany(Long companyId) {
        if (companyMapper.selectById(companyId) == null) {
            throw BizException.validate("所属企业不存在：id=" + companyId);
        }
    }

    private List<AdminMentorVO> toVOs(List<Mentor> mentors) {
        if (mentors == null || mentors.isEmpty()) return new ArrayList<>();
        List<Long> userIds = mentors.stream().map(Mentor::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        List<Long> companyIds = mentors.stream().map(Mentor::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? new HashMap<>()
                : companyMapper.selectByIds(companyIds).stream().collect(Collectors.toMap(Company::getId, c -> c));

        List<AdminMentorVO> result = new ArrayList<>(mentors.size());
        for (Mentor m : mentors) {
            SysUser u = userMap.get(m.getUserId());
            Company c = companyMap.get(m.getCompanyId());
            AdminMentorVO vo = new AdminMentorVO();
            vo.setId(m.getId());
            vo.setUserId(m.getUserId());
            vo.setUsername(u == null ? null : u.getUsername());
            vo.setRealName(u == null ? null : u.getRealName());
            vo.setPhone(u == null ? null : u.getPhone());
            vo.setStatus(u == null ? null : u.getStatus());
            vo.setCompanyId(m.getCompanyId());
            vo.setCompanyName(c == null ? null : c.getName());
            vo.setPosition(m.getPosition());
            vo.setDept(m.getDept());
            vo.setCreateTime(m.getCreateTime());
            vo.setStudentCount(studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getMentorId, m.getId())));
            result.add(vo);
        }
        return result;
    }
}
