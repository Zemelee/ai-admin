package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.common.BizException;
import com.dto.admin.AdminCompanySaveReq;
import com.dto.admin.AdminCompanyVO;
import com.entity.Company;
import com.mapper.CompanyMapper;
import com.mapper.MentorMapper;
import com.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 监管者维护实习合作企业档案。
 */
@Service
@RequiredArgsConstructor
public class AdminCompanyService {

    private final CompanyMapper companyMapper;
    private final MentorMapper mentorMapper;
    private final StudentMapper studentMapper;

    public IPage<AdminCompanyVO> page(String keyword, Integer isBlacklist, long pageNo, long size) {
        Page<Company> page = new Page<>(pageNo, size);
        LambdaQueryWrapper<Company> qw = new LambdaQueryWrapper<Company>()
                .like(StrUtil.isNotBlank(keyword), Company::getName, keyword)
                .eq(isBlacklist != null, Company::getIsBlacklist, isBlacklist)
                .orderByDesc(Company::getCreateTime);
        IPage<Company> companyPage = companyMapper.selectPage(page, qw);

        List<AdminCompanyVO> vos = toVOs(companyPage.getRecords());
        Page<AdminCompanyVO> result = new Page<>(pageNo, size, companyPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    public Long create(AdminCompanySaveReq req) {
        requireUniqueSocialCode(req.getSocialCode(), null);
        Company c = new Company();
        copy(c, req);
        companyMapper.insert(c);
        return c.getId();
    }

    public void update(Long id, AdminCompanySaveReq req) {
        Company c = companyMapper.selectById(id);
        if (c == null) {
            throw BizException.notFound("企业不存在：id=" + id);
        }
        requireUniqueSocialCode(req.getSocialCode(), id);
        copy(c, req);
        companyMapper.updateById(c);
    }

    public void delete(Long id) {
        Company c = companyMapper.selectById(id);
        if (c == null) return;
        // 企业下仍有 mentor 或在岗学生时不允许删除
        if (mentorMapper.selectCount(new LambdaQueryWrapper<com.entity.Mentor>()
                .eq(com.entity.Mentor::getCompanyId, id)) > 0) {
            throw BizException.validate("该企业下仍有企业指导人员，无法删除");
        }
        if (studentMapper.selectCount(new LambdaQueryWrapper<com.entity.Student>()
                .eq(com.entity.Student::getCompanyId, id)) > 0) {
            throw BizException.validate("该企业下仍有实习学生，无法删除");
        }
        companyMapper.deleteById(id);
    }

    // ---------- 内部 ----------

    private void copy(Company c, AdminCompanySaveReq req) {
        c.setName(req.getName());
        c.setSocialCode(req.getSocialCode());
        c.setAddress(req.getAddress());
        c.setIndustry(req.getIndustry());
        c.setContactPerson(req.getContactPerson());
        c.setContactPhone(req.getContactPhone());
        c.setIsBlacklist(req.getIsBlacklist() == null ? 0 : req.getIsBlacklist());
        c.setBlacklistReason(c.getIsBlacklist() == 1 ? req.getBlacklistReason() : null);
        c.setRemark(req.getRemark());
    }

    private void requireUniqueSocialCode(String socialCode, Long excludeId) {
        if (StrUtil.isBlank(socialCode)) return;
        LambdaQueryWrapper<Company> qw = new LambdaQueryWrapper<Company>()
                .eq(Company::getSocialCode, socialCode);
        if (excludeId != null) qw.ne(Company::getId, excludeId);
        if (companyMapper.selectCount(qw) > 0) {
            throw BizException.validate("信用代码已存在：" + socialCode);
        }
    }

    private List<AdminCompanyVO> toVOs(List<Company> companies) {
        if (companies == null || companies.isEmpty()) return new ArrayList<>();
        List<AdminCompanyVO> vos = new ArrayList<>(companies.size());
        for (Company c : companies) {
            AdminCompanyVO vo = new AdminCompanyVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setSocialCode(c.getSocialCode());
            vo.setAddress(c.getAddress());
            vo.setIndustry(c.getIndustry());
            vo.setContactPerson(c.getContactPerson());
            vo.setContactPhone(c.getContactPhone());
            vo.setIsBlacklist(c.getIsBlacklist());
            vo.setBlacklistReason(c.getBlacklistReason());
            vo.setRemark(c.getRemark());
            vo.setCreateTime(c.getCreateTime());
            vo.setMentorCount(mentorMapper.selectCount(new LambdaQueryWrapper<com.entity.Mentor>()
                    .eq(com.entity.Mentor::getCompanyId, c.getId())));
            vo.setStudentCount(studentMapper.selectCount(new LambdaQueryWrapper<com.entity.Student>()
                    .eq(com.entity.Student::getCompanyId, c.getId())));
            vos.add(vo);
        }
        return vos;
    }
}
