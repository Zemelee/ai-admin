package com.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.BizException;
import com.common.TransferConst;
import com.dto.attachment.AttachmentVO;
import com.dto.transfer.TransferApproveReq;
import com.dto.transfer.TransferDetailVO;
import com.dto.transfer.TransferListItemVO;
import com.dto.transfer.TransferSubmitReq;
import com.entity.ApprovalFlow;
import com.entity.Company;
import com.entity.Mentor;
import com.entity.Student;
import com.entity.SysUser;
import com.entity.Teacher;
import com.entity.TransferApply;
import com.mapper.ApprovalFlowMapper;
import com.mapper.CompanyMapper;
import com.mapper.MentorMapper;
import com.mapper.StudentMapper;
import com.mapper.SysUserMapper;
import com.mapper.TeacherMapper;
import com.mapper.TransferApplyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实习单位变更业务核心服务：学生提交、三端串行审批（企业指导 → 教师 → 监管者）。
 * <p>
 * MVP 约束：
 * - 新企业命中黑名单 / 缺失 → 红色阻断（验证层抛 422）
 * - 原单位实习时长前置：总实习期 ≤ 半年 需满 1 个月；> 半年 需满 3 个月
 * - 同一学生不可同时存在两条 PENDING
 * - 终态 APPROVED 时回写 student.companyId（toCompanyId 非空才回写，外部企业仅留台账）
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferApplyMapper transferMapper;
    private final ApprovalFlowMapper approvalFlowMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;
    private final TeacherMapper teacherMapper;
    private final MentorMapper mentorMapper;
    private final LeaveQueryHelper helper;
    private final AttachmentService attachmentService;

    // ========== 学生端 ==========

    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, TransferSubmitReq req) {
        Student student = helper.requireStudentByUserId(userId);
        if (student.getCompanyId() == null) {
            throw BizException.validate("尚未绑定实习单位，无需发起变更");
        }
        // 不允许重复在途
        long pendingCount = transferMapper.selectCount(new LambdaQueryWrapper<TransferApply>()
                .eq(TransferApply::getStudentId, student.getId())
                .eq(TransferApply::getStatus, TransferConst.STATUS_PENDING));
        if (pendingCount > 0) {
            throw BizException.validate("已有处理中的变更申请，请等待结果或先撤回");
        }

        // 新单位校验
        String toName = validateAndResolveNewCompany(req, student);

        // 原单位实习时长前置校验
        validateOriginDuration(student);

        TransferApply tr = new TransferApply();
        tr.setStudentId(student.getId());
        tr.setFromCompanyId(student.getCompanyId());
        tr.setToCompanyId(req.getToCompanyId());
        tr.setToCompanyName(toName);
        tr.setReason(req.getReason());
        tr.setExpectedStart(req.getExpectedStart());
        tr.setStatus(TransferConst.STATUS_PENDING);
        tr.setCurrentNode(TransferConst.NODE_MENTOR);
        tr.setSubmitTime(LocalDateTime.now());
        transferMapper.insert(tr);

        // 首节点：原企业 mentor
        ApprovalFlow mf = new ApprovalFlow();
        mf.setBizType(TransferConst.BIZ_TRANSFER);
        mf.setBizId(tr.getId());
        mf.setNode(TransferConst.NODE_MENTOR);
        mf.setNodeSeq(1);
        if (student.getMentorId() != null) {
            Mentor mentor = mentorMapper.selectById(student.getMentorId());
            if (mentor != null) {
                mf.setApproverId(mentor.getUserId());
                SysUser mu = sysUserMapper.selectById(mentor.getUserId());
                if (mu != null) mf.setApproverName(mu.getRealName());
            }
        }
        approvalFlowMapper.insert(mf);

        attachmentService.bindBiz(userId, TransferConst.BIZ_TRANSFER, tr.getId(), req.getAttachmentIds());
        return tr.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long transferId) {
        Student student = helper.requireStudentByUserId(userId);
        TransferApply tr = requireTransfer(transferId);
        if (!Objects.equals(tr.getStudentId(), student.getId())) {
            throw BizException.forbidden("无权操作他人申请单");
        }
        if (!TransferConst.STATUS_PENDING.equals(tr.getStatus())) {
            throw BizException.validate("仅待审批状态可撤回，当前状态：" + tr.getStatus());
        }
        tr.setStatus(TransferConst.STATUS_CANCELLED);
        tr.setCurrentNode(null);
        tr.setFinishTime(LocalDateTime.now());
        transferMapper.updateById(tr);
    }

    public List<TransferListItemVO> myTransfers(Long userId, String status) {
        Student student = helper.requireStudentByUserId(userId);
        LambdaQueryWrapper<TransferApply> qw = new LambdaQueryWrapper<TransferApply>()
                .eq(TransferApply::getStudentId, student.getId())
                .orderByDesc(TransferApply::getCreateTime);
        if (StrUtil.isNotBlank(status)) qw.eq(TransferApply::getStatus, status);
        return enrichListItems(transferMapper.selectList(qw));
    }

    // ========== 各角色待审批 / 历史 ==========

    public List<TransferListItemVO> pendingForApprover(Long userId, String expectNode) {
        List<ApprovalFlow> flows = approvalFlowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, TransferConst.BIZ_TRANSFER)
                .eq(ApprovalFlow::getNode, expectNode)
                .isNull(ApprovalFlow::getResult)
                .and(w -> {
                    // MENTOR/TEACHER 节点：必须是指派给自己的；SUPERVISOR 节点：所有 supervisor 共享待办
                    if (TransferConst.NODE_SUPERVISOR.equals(expectNode)) {
                        w.isNull(ApprovalFlow::getApproverId).or().eq(ApprovalFlow::getApproverId, userId);
                    } else {
                        w.eq(ApprovalFlow::getApproverId, userId);
                    }
                }));
        if (flows.isEmpty()) return new ArrayList<>();
        List<Long> bizIds = flows.stream().map(ApprovalFlow::getBizId).distinct().collect(Collectors.toList());
        List<TransferApply> list = transferMapper.selectList(new LambdaQueryWrapper<TransferApply>()
                .in(TransferApply::getId, bizIds)
                .eq(TransferApply::getStatus, TransferConst.STATUS_PENDING)
                .eq(TransferApply::getCurrentNode, expectNode)
                .orderByAsc(TransferApply::getSubmitTime));
        return enrichListItems(list);
    }

    public List<TransferListItemVO> approvedHistoryByMe(Long userId) {
        List<ApprovalFlow> flows = approvalFlowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, TransferConst.BIZ_TRANSFER)
                .eq(ApprovalFlow::getApproverId, userId)
                .isNotNull(ApprovalFlow::getResult)
                .orderByDesc(ApprovalFlow::getActTime));
        if (flows.isEmpty()) return new ArrayList<>();
        List<Long> bizIds = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ApprovalFlow f : flows) {
            if (seen.add(f.getBizId())) bizIds.add(f.getBizId());
        }
        List<TransferApply> all = transferMapper.selectByIds(bizIds);
        Map<Long, TransferApply> map = all.stream().collect(Collectors.toMap(TransferApply::getId, x -> x));
        List<TransferApply> ordered = new ArrayList<>(bizIds.size());
        for (Long id : bizIds) {
            TransferApply t = map.get(id);
            if (t != null) ordered.add(t);
        }
        return enrichListItems(ordered);
    }

    // ========== 审批推进 ==========

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long userId, Long transferId, String expectNode, TransferApproveReq req) {
        if (!TransferConst.RESULT_APPROVED.equals(req.getResult())
                && !TransferConst.RESULT_REJECTED.equals(req.getResult())) {
            throw BizException.validate("非法的审批结果：" + req.getResult());
        }
        if (TransferConst.RESULT_REJECTED.equals(req.getResult()) && StrUtil.isBlank(req.getComment())) {
            throw BizException.validate("驳回必须填写审批意见");
        }

        TransferApply tr = requireTransfer(transferId);
        if (!TransferConst.STATUS_PENDING.equals(tr.getStatus())) {
            throw BizException.validate("当前申请已不在待审批状态");
        }
        if (!expectNode.equals(tr.getCurrentNode())) {
            throw BizException.forbidden("当前节点为 " + tr.getCurrentNode() + "，无权审批");
        }

        ApprovalFlow flow = currentPendingFlow(transferId, expectNode);
        if (flow == null) throw BizException.validate("未找到该节点的待办流水（数据异常）");

        // MENTOR/TEACHER：必须是预先指派的本人；SUPERVISOR：抢占式领取
        if (TransferConst.NODE_SUPERVISOR.equals(expectNode)) {
            flow.setApproverId(userId);
        } else {
            if (flow.getApproverId() != null && !Objects.equals(flow.getApproverId(), userId)) {
                throw BizException.forbidden("该单已指派给其他审批人");
            }
            if (flow.getApproverId() == null) flow.setApproverId(userId);
        }

        SysUser actor = sysUserMapper.selectById(userId);
        if (actor != null) flow.setApproverName(actor.getRealName());
        flow.setResult(req.getResult());
        flow.setComment(req.getComment());
        flow.setActTime(LocalDateTime.now());
        approvalFlowMapper.updateById(flow);

        if (TransferConst.RESULT_REJECTED.equals(req.getResult())) {
            tr.setStatus(TransferConst.STATUS_REJECTED);
            tr.setCurrentNode(null);
            tr.setFinishTime(LocalDateTime.now());
            transferMapper.updateById(tr);
            return;
        }

        // 通过：根据当前节点推进
        String next = nextNode(expectNode);
        if (next != null) {
            tr.setCurrentNode(next);
            transferMapper.updateById(tr);

            ApprovalFlow nf = new ApprovalFlow();
            nf.setBizType(TransferConst.BIZ_TRANSFER);
            nf.setBizId(tr.getId());
            nf.setNode(next);
            nf.setNodeSeq(flow.getNodeSeq() + 1);
            // TEACHER 节点指派给学生当前分管教师；SUPERVISOR 节点不预指派
            if (TransferConst.NODE_TEACHER.equals(next)) {
                Student student = studentMapper.selectById(tr.getStudentId());
                if (student != null && student.getTeacherId() != null) {
                    Teacher teacher = teacherMapper.selectById(student.getTeacherId());
                    if (teacher != null) {
                        nf.setApproverId(teacher.getUserId());
                        SysUser tu = sysUserMapper.selectById(teacher.getUserId());
                        if (tu != null) nf.setApproverName(tu.getRealName());
                    }
                }
            }
            approvalFlowMapper.insert(nf);
            return;
        }

        // 已是终审（SUPERVISOR 通过）→ APPROVED，回写学生绑定关系
        tr.setStatus(TransferConst.STATUS_APPROVED);
        tr.setCurrentNode(null);
        tr.setFinishTime(LocalDateTime.now());
        transferMapper.updateById(tr);
        applyFinalApprovedSideEffect(tr);
    }

    // ========== 详情 ==========

    public TransferDetailVO detail(Long userId, String roleCode, Long id) {
        TransferApply tr = requireTransfer(id);
        checkDetailAccess(tr, userId, roleCode);
        return buildDetail(tr);
    }

    // ========== 内部 ==========

    private String nextNode(String current) {
        if (TransferConst.NODE_MENTOR.equals(current)) return TransferConst.NODE_TEACHER;
        if (TransferConst.NODE_TEACHER.equals(current)) return TransferConst.NODE_SUPERVISOR;
        return null;
    }

    private String validateAndResolveNewCompany(TransferSubmitReq req, Student student) {
        if (req.getToCompanyId() != null) {
            if (Objects.equals(req.getToCompanyId(), student.getCompanyId())) {
                throw BizException.validate("新单位不能与原单位相同");
            }
            Company c = companyMapper.selectById(req.getToCompanyId());
            if (c == null) throw BizException.validate("新单位不存在：id=" + req.getToCompanyId());
            if (c.getIsBlacklist() != null && c.getIsBlacklist() == 1) {
                throw BizException.validate("新单位在禁入清单中，无法变更");
            }
            return c.getName();
        }
        if (StrUtil.isBlank(req.getToCompanyName())) {
            throw BizException.validate("未选择系统内企业时，必须填写新单位名称");
        }
        return req.getToCompanyName().trim();
    }

    private void validateOriginDuration(Student student) {
        if (student.getInternStart() == null) {
            // 缺数据时不阻断，但提示
            return;
        }
        LocalDate today = LocalDate.now();
        long daysAtOrigin = ChronoUnit.DAYS.between(student.getInternStart(), today);
        // 总实习期：以 internStart..internEnd 计算；缺失则按 6 个月以内估算
        long totalDays;
        if (student.getInternEnd() != null) {
            totalDays = ChronoUnit.DAYS.between(student.getInternStart(), student.getInternEnd());
        } else {
            totalDays = TransferConst.TOTAL_HALF_YEAR_DAYS;
        }
        int minAtOrigin = totalDays > TransferConst.TOTAL_HALF_YEAR_DAYS
                ? TransferConst.MIN_AT_ORIGIN_OVER_HALF_YEAR
                : TransferConst.MIN_AT_ORIGIN_HALF_YEAR;
        if (daysAtOrigin < minAtOrigin) {
            throw BizException.validate(String.format(
                    "原单位实习天数 %d 不足 %d 天，不允许变更（总实习期 %s 半年）",
                    daysAtOrigin, minAtOrigin,
                    totalDays > TransferConst.TOTAL_HALF_YEAR_DAYS ? "超过" : "≤"));
        }
    }

    private void applyFinalApprovedSideEffect(TransferApply tr) {
        if (tr.getToCompanyId() == null) {
            // 外部企业：仅留台账，不动 student 绑定
            return;
        }
        Student student = studentMapper.selectById(tr.getStudentId());
        if (student == null) return;
        student.setCompanyId(tr.getToCompanyId());
        // 切换企业，原 mentor 不再适用 → 解绑（等待管理员重新指派）
        student.setMentorId(null);
        studentMapper.updateById(student);
    }

    private TransferApply requireTransfer(Long id) {
        TransferApply t = transferMapper.selectById(id);
        if (t == null) throw BizException.notFound("单位变更申请不存在：id=" + id);
        return t;
    }

    private ApprovalFlow currentPendingFlow(Long bizId, String node) {
        return approvalFlowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, TransferConst.BIZ_TRANSFER)
                .eq(ApprovalFlow::getBizId, bizId)
                .eq(ApprovalFlow::getNode, node)
                .isNull(ApprovalFlow::getResult)
                .last("LIMIT 1"));
    }

    private void checkDetailAccess(TransferApply tr, Long userId, String roleCode) {
        if ("supervisor".equals(roleCode)) return;
        if ("student".equals(roleCode)) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getUserId, userId).last("LIMIT 1"));
            if (s != null && Objects.equals(s.getId(), tr.getStudentId())) return;
        }
        if ("teacher".equals(roleCode) || "mentor".equals(roleCode)) {
            long n = approvalFlowMapper.selectCount(new LambdaQueryWrapper<ApprovalFlow>()
                    .eq(ApprovalFlow::getBizType, TransferConst.BIZ_TRANSFER)
                    .eq(ApprovalFlow::getBizId, tr.getId())
                    .eq(ApprovalFlow::getApproverId, userId));
            if (n > 0) return;
        }
        throw BizException.forbidden("无权查看该申请单");
    }

    private List<TransferListItemVO> enrichListItems(List<TransferApply> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        List<Long> studentIds = list.stream().map(TransferApply::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Student> studentMap = studentMapper.selectByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, x -> x));
        List<Long> userIds = studentMap.values().stream().map(Student::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        // 企业名（原 + 新）
        Set<Long> companyIds = new HashSet<>();
        for (TransferApply t : list) {
            if (t.getFromCompanyId() != null) companyIds.add(t.getFromCompanyId());
            if (t.getToCompanyId() != null) companyIds.add(t.getToCompanyId());
        }
        Map<Long, Company> companyMap = companyIds.isEmpty() ? new HashMap<>()
                : companyMapper.selectByIds(companyIds).stream().collect(Collectors.toMap(Company::getId, c -> c));

        List<TransferListItemVO> result = new ArrayList<>(list.size());
        for (TransferApply t : list) {
            TransferListItemVO vo = new TransferListItemVO();
            vo.setId(t.getId());
            vo.setStudentId(t.getStudentId());
            Student s = studentMap.get(t.getStudentId());
            if (s != null) {
                vo.setStudentNo(s.getStudentNo());
                vo.setClassName(s.getClassName());
                SysUser u = userMap.get(s.getUserId());
                if (u != null) vo.setStudentName(u.getRealName());
            }
            vo.setFromCompanyId(t.getFromCompanyId());
            Company fc = t.getFromCompanyId() == null ? null : companyMap.get(t.getFromCompanyId());
            vo.setFromCompanyName(fc == null ? null : fc.getName());
            vo.setToCompanyId(t.getToCompanyId());
            // 优先用快照（外部企业必有），系统内企业回显最新名称
            if (t.getToCompanyId() != null) {
                Company tc = companyMap.get(t.getToCompanyId());
                vo.setToCompanyName(tc == null ? t.getToCompanyName() : tc.getName());
            } else {
                vo.setToCompanyName(t.getToCompanyName());
            }
            vo.setExpectedStart(t.getExpectedStart());
            vo.setReason(t.getReason());
            vo.setStatus(t.getStatus());
            vo.setCurrentNode(t.getCurrentNode());
            vo.setSubmitTime(t.getSubmitTime());
            vo.setCreateTime(t.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private TransferDetailVO buildDetail(TransferApply tr) {
        Student student = studentMapper.selectById(tr.getStudentId());
        SysUser user = student == null ? null : sysUserMapper.selectById(student.getUserId());
        Company from = tr.getFromCompanyId() == null ? null : companyMapper.selectById(tr.getFromCompanyId());
        Company to = tr.getToCompanyId() == null ? null : companyMapper.selectById(tr.getToCompanyId());

        TransferDetailVO vo = new TransferDetailVO();
        vo.setId(tr.getId());
        vo.setStudentId(tr.getStudentId());
        vo.setStudentName(user == null ? null : user.getRealName());
        vo.setStudentNo(student == null ? null : student.getStudentNo());
        vo.setClassName(student == null ? null : student.getClassName());
        vo.setMajor(student == null ? null : student.getMajor());
        vo.setFromCompanyId(tr.getFromCompanyId());
        vo.setFromCompanyName(from == null ? null : from.getName());
        vo.setToCompanyId(tr.getToCompanyId());
        vo.setToCompanyName(to == null ? tr.getToCompanyName() : to.getName());
        vo.setReason(tr.getReason());
        vo.setExpectedStart(tr.getExpectedStart());
        vo.setStatus(tr.getStatus());
        vo.setCurrentNode(tr.getCurrentNode());
        vo.setSubmitTime(tr.getSubmitTime());
        vo.setFinishTime(tr.getFinishTime());
        vo.setCreateTime(tr.getCreateTime());

        List<ApprovalFlow> flows = approvalFlowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getBizType, TransferConst.BIZ_TRANSFER)
                .eq(ApprovalFlow::getBizId, tr.getId())
                .orderByAsc(ApprovalFlow::getNodeSeq));
        List<TransferDetailVO.FlowItem> items = new ArrayList<>(flows.size());
        for (ApprovalFlow f : flows) {
            TransferDetailVO.FlowItem fi = new TransferDetailVO.FlowItem();
            fi.setId(f.getId());
            fi.setNode(f.getNode());
            fi.setNodeSeq(f.getNodeSeq());
            fi.setApproverId(f.getApproverId());
            fi.setApproverName(f.getApproverName());
            fi.setResult(f.getResult());
            fi.setComment(f.getComment());
            fi.setActTime(f.getActTime());
            items.add(fi);
        }
        vo.setFlow(items);

        List<AttachmentVO> attachments = attachmentService.listByBiz(TransferConst.BIZ_TRANSFER, tr.getId());
        vo.setAttachments(attachments);
        return vo;
    }
}
