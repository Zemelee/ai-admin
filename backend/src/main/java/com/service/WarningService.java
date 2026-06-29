package com.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.WarningConst;
import com.dto.warning.WarningReviewReq;
import com.dto.warning.WarningStatsVO;
import com.dto.warning.WarningVO;
import com.entity.Company;
import com.entity.InternLog;
import com.entity.InternWeekly;
import com.entity.LeaveApply;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.TransferApply;
import com.entity.Warning;
import com.mapper.CompanyMapper;
import com.mapper.InternLogMapper;
import com.mapper.InternWeeklyMapper;
import com.mapper.LeaveApplyMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TransferApplyMapper;
import com.mapper.WarningMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 三色预警服务。
 * <p>规则对齐学院实习管理办法用工红线与考勤纪律：
 * <ul>
 *   <li>NO_LOG_3D（黄）：ACTIVE 学生超过 3 天未提交实习日志</li>
 *   <li>SENSITIVE_WORD（红）：日志/周记命中用工红线敏感词</li>
 *   <li>COMPANY_BLACKLIST（红）：学生当前所在企业属禁入清单</li>
 *   <li>TRANSFER_PENDING_3D（黄）：单位变更申请停滞超过 3 天</li>
 *   <li>LEAVE_OVER_3M（黄）：请假累计超过 3 个月</li>
 * </ul>
 * 扫描幂等：相同 (ruleCode, bizId, studentId, status=PENDING) 不重复入库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarningService {

    private final WarningMapper warningMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;
    private final InternLogMapper internLogMapper;
    private final InternWeeklyMapper internWeeklyMapper;
    private final LeaveApplyMapper leaveApplyMapper;
    private final TransferApplyMapper transferApplyMapper;

    // ========== 扫描 ==========

    @Transactional(rollbackFor = Exception.class)
    public int scan() {
        int count = 0;
        count += scanNoLog();
        count += scanSensitive();
        count += scanCompanyBlacklist();
        count += scanTransferPending();
        count += scanLeaveOver();
        return count;
    }

    /** NO_LOG_3D：ACTIVE 学生最近一条日志距今超过阈值 */
    private int scanNoLog() {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getInternStatus, "ACTIVE"));
        LocalDate threshold = LocalDate.now().minusDays(WarningConst.NO_LOG_DAYS);
        int n = 0;
        for (Student s : students) {
            InternLog latest = internLogMapper.selectOne(new LambdaQueryWrapper<InternLog>()
                    .eq(InternLog::getStudentId, s.getId())
                    .orderByDesc(InternLog::getLogDate)
                    .last("LIMIT 1"));
            LocalDate ref = latest == null ? null : latest.getLogDate();
            // 无日志时以实习开始日为基准；若实习尚未开始则跳过
            if (ref == null) {
                if (s.getInternStart() == null) continue;
                ref = s.getInternStart();
            }
            if (ref.isBefore(threshold)) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(ref, LocalDate.now());
                if (upsert(WarningConst.LEVEL_YELLOW, WarningConst.RULE_NO_LOG_3D,
                        "学生超过 " + WarningConst.NO_LOG_DAYS + " 天未提交实习日志",
                        s, null, null, "已 " + days + " 天未提交日志")) {
                    n++;
                }
            }
        }
        return n;
    }

    /** SENSITIVE_WORD：日志/周记命中敏感词（每条业务单一条预警） */
    private int scanSensitive() {
        int n = 0;
        // 日志
        List<InternLog> logs = internLogMapper.selectList(new LambdaQueryWrapper<InternLog>()
                .eq(InternLog::getSensitiveHit, 1));
        for (InternLog l : logs) {
            Student s = studentMapper.selectById(l.getStudentId());
            if (s == null) continue;
            if (upsert(WarningConst.LEVEL_RED, WarningConst.RULE_SENSITIVE_WORD,
                    "实习日志命中用工红线敏感词",
                    s, "LOG", l.getId(), l.getSensitiveWords())) {
                n++;
            }
        }
        // 周记
        List<InternWeekly> weeklies = internWeeklyMapper.selectList(new LambdaQueryWrapper<InternWeekly>()
                .eq(InternWeekly::getSensitiveHit, 1));
        for (InternWeekly w : weeklies) {
            Student s = studentMapper.selectById(w.getStudentId());
            if (s == null) continue;
            if (upsert(WarningConst.LEVEL_RED, WarningConst.RULE_SENSITIVE_WORD,
                    "实习周记命中用工红线敏感词",
                    s, "WEEKLY", w.getId(), w.getSensitiveWords())) {
                n++;
            }
        }
        return n;
    }

    /** COMPANY_BLACKLIST：学生当前企业属禁入清单 */
    private int scanCompanyBlacklist() {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .isNotNull(Student::getCompanyId));
        int n = 0;
        for (Student s : students) {
            if (s.getCompanyId() == null) continue;
            Company c = companyMapper.selectById(s.getCompanyId());
            if (c != null && c.getIsBlacklist() != null && c.getIsBlacklist() == 1) {
                if (upsert(WarningConst.LEVEL_RED, WarningConst.RULE_COMPANY_BLACKLIST,
                        "学生所在企业属禁入清单",
                        s, "COMPANY", c.getId(), c.getName())) {
                    n++;
                }
            }
        }
        return n;
    }

    /** TRANSFER_PENDING_3D：单位变更申请停滞超阈值 */
    private int scanTransferPending() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(WarningConst.TRANSFER_PENDING_DAYS);
        List<TransferApply> list = transferApplyMapper.selectList(new LambdaQueryWrapper<TransferApply>()
                .eq(TransferApply::getStatus, "PENDING")
                .lt(TransferApply::getSubmitTime, threshold));
        int n = 0;
        for (TransferApply t : list) {
            Student s = studentMapper.selectById(t.getStudentId());
            if (s == null) continue;
            if (upsert(WarningConst.LEVEL_YELLOW, WarningConst.RULE_TRANSFER_PENDING_3D,
                    "单位变更申请停滞超过 " + WarningConst.TRANSFER_PENDING_DAYS + " 天",
                    s, "TRANSFER", t.getId(), "当前节点：" + t.getCurrentNode())) {
                n++;
            }
        }
        return n;
    }

    /** LEAVE_OVER_3M：已通过请假累计超过 3 个月 */
    private int scanLeaveOver() {
        BigDecimal limit = new BigDecimal(WarningConst.LEAVE_LONG_DAYS);
        List<LeaveApply> list = leaveApplyMapper.selectList(new LambdaQueryWrapper<LeaveApply>()
                .eq(LeaveApply::getStatus, "APPROVED")
                .ge(LeaveApply::getDurationDays, limit));
        int n = 0;
        for (LeaveApply l : list) {
            Student s = studentMapper.selectById(l.getStudentId());
            if (s == null) continue;
            if (upsert(WarningConst.LEVEL_YELLOW, WarningConst.RULE_LEAVE_OVER_3M,
                    "请假累计超过 " + WarningConst.LEAVE_LONG_DAYS + " 天",
                    s, "LEAVE", l.getId(), "请假 " + l.getDurationDays() + " 天")) {
                n++;
            }
        }
        return n;
    }

    /**
     * 幂等写入。同 (ruleCode, bizId, studentId) 已存在（任意状态）则跳过，避免审核后重复生成。
     * <p>注意：不限定 status。若限定 PENDING，则审核为 REVIEWED/IGNORED 后状态变更，
     * 下次扫描会误判为"不存在"而重新生成，导致已处理事件反复出现。
     *
     * @return true 表示新增
     */
    private boolean upsert(String level, String ruleCode, String ruleDesc,
                           Student student, String bizType, Long bizId, String detail) {
        LambdaQueryWrapper<Warning> qw = new LambdaQueryWrapper<Warning>()
                .eq(Warning::getRuleCode, ruleCode)
                .eq(Warning::getStudentId, student.getId());
        if (bizId == null) {
            qw.isNull(Warning::getBizId);
        } else {
            qw.eq(Warning::getBizId, bizId);
        }
        Long exists = warningMapper.selectCount(qw);
        if (exists != null && exists > 0) return false;

        Warning w = new Warning();
        w.setLevel(level);
        w.setRuleCode(ruleCode);
        w.setRuleDesc(ruleDesc);
        w.setStudentId(student.getId());
        w.setStudentName(studentName(student));
        w.setBizType(bizType);
        w.setBizId(bizId);
        w.setDetail(detail);
        w.setStatus(WarningConst.STATUS_PENDING);
        warningMapper.insert(w);
        return true;
    }

    private String studentName(Student s) {
        if (s == null || s.getUserId() == null) return null;
        SysUser u = sysUserMapper.selectById(s.getUserId());
        return u == null ? null : u.getRealName();
    }

    // ========== 查询 ==========

    /** 列表（先扫描保持新鲜） */
    public List<WarningVO> list(String level, String status, String ruleCode) {
        scan();
        LambdaQueryWrapper<Warning> qw = new LambdaQueryWrapper<Warning>()
                .orderByDesc(Warning::getLevel)
                .orderByDesc(Warning::getCreateTime);
        if (level != null && !level.isEmpty()) qw.eq(Warning::getLevel, level);
        if (status != null && !status.isEmpty()) qw.eq(Warning::getStatus, status);
        else qw.eq(Warning::getStatus, WarningConst.STATUS_PENDING);
        if (ruleCode != null && !ruleCode.isEmpty()) qw.eq(Warning::getRuleCode, ruleCode);

        List<Warning> list = warningMapper.selectList(qw);
        if (list.isEmpty()) return new ArrayList<>();

        // 批量补学生信息
        List<Long> studentIds = list.stream().map(Warning::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, x -> x));
        List<Long> reviewerIds = list.stream().map(Warning::getReviewerId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> reviewerMap = reviewerIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(reviewerIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<WarningVO> result = new ArrayList<>(list.size());
        for (Warning w : list) {
            WarningVO vo = new WarningVO();
            vo.setId(w.getId());
            vo.setLevel(w.getLevel());
            vo.setRuleCode(w.getRuleCode());
            vo.setRuleDesc(w.getRuleDesc());
            vo.setStudentId(w.getStudentId());
            vo.setStudentName(w.getStudentName());
            vo.setBizType(w.getBizType());
            vo.setBizId(w.getBizId());
            vo.setDetail(w.getDetail());
            vo.setStatus(w.getStatus());
            vo.setReviewerId(w.getReviewerId());
            vo.setReviewTime(w.getReviewTime());
            vo.setReviewNote(w.getReviewNote());
            vo.setCreateTime(w.getCreateTime());
            Student s = studentMap.get(w.getStudentId());
            if (s != null) {
                vo.setStudentNo(s.getStudentNo());
                vo.setClassName(s.getClassName());
                vo.setMajor(s.getMajor());
            }
            SysUser rv = reviewerMap.get(w.getReviewerId());
            if (rv != null) vo.setReviewerName(rv.getRealName());
            result.add(vo);
        }
        return result;
    }

    public WarningStatsVO stats() {
        scan();
        WarningStatsVO vo = new WarningStatsVO();
        Map<String, Long> levelMap = new LinkedHashMap<>();
        levelMap.put(WarningConst.LEVEL_RED, countPendingByLevel(WarningConst.LEVEL_RED));
        levelMap.put(WarningConst.LEVEL_YELLOW, countPendingByLevel(WarningConst.LEVEL_YELLOW));
        vo.setLevelMap(levelMap);

        Map<String, Long> ruleMap = new LinkedHashMap<>();
        ruleMap.put(WarningConst.RULE_NO_LOG_3D, countPendingByRule(WarningConst.RULE_NO_LOG_3D));
        ruleMap.put(WarningConst.RULE_SENSITIVE_WORD, countPendingByRule(WarningConst.RULE_SENSITIVE_WORD));
        ruleMap.put(WarningConst.RULE_COMPANY_BLACKLIST, countPendingByRule(WarningConst.RULE_COMPANY_BLACKLIST));
        ruleMap.put(WarningConst.RULE_TRANSFER_PENDING_3D, countPendingByRule(WarningConst.RULE_TRANSFER_PENDING_3D));
        ruleMap.put(WarningConst.RULE_LEAVE_OVER_3M, countPendingByRule(WarningConst.RULE_LEAVE_OVER_3M));
        vo.setRuleMap(ruleMap);

        vo.setPendingTotal(warningMapper.selectCount(new LambdaQueryWrapper<Warning>()
                .eq(Warning::getStatus, WarningConst.STATUS_PENDING)));
        vo.setHandledTotal(warningMapper.selectCount(new LambdaQueryWrapper<Warning>()
                .ne(Warning::getStatus, WarningConst.STATUS_PENDING)));
        return vo;
    }

    private Long countPendingByLevel(String level) {
        return warningMapper.selectCount(new LambdaQueryWrapper<Warning>()
                .eq(Warning::getLevel, level)
                .eq(Warning::getStatus, WarningConst.STATUS_PENDING));
    }

    private Long countPendingByRule(String rule) {
        return warningMapper.selectCount(new LambdaQueryWrapper<Warning>()
                .eq(Warning::getRuleCode, rule)
                .eq(Warning::getStatus, WarningConst.STATUS_PENDING));
    }

    // ========== 审核 ==========

    @Transactional(rollbackFor = Exception.class)
    public void review(Long userId, Long id, WarningReviewReq req) {
        if (!WarningConst.STATUS_REVIEWED.equals(req.getStatus())
                && !WarningConst.STATUS_IGNORED.equals(req.getStatus())) {
            throw BizException.validate("非法的审核状态：" + req.getStatus());
        }
        Warning w = warningMapper.selectById(id);
        if (w == null) throw BizException.notFound("预警事件不存在：id=" + id);
        if (!WarningConst.STATUS_PENDING.equals(w.getStatus())) {
            throw BizException.validate("该预警已处理");
        }
        w.setStatus(req.getStatus());
        w.setReviewNote(req.getReviewNote());
        w.setReviewerId(userId);
        w.setReviewTime(LocalDateTime.now());
        warningMapper.updateById(w);
    }
}
