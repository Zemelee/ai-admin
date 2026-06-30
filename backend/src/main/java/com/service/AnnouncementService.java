package com.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.common.BizException;
import com.entity.Announcement;
import com.entity.SysUser;
import com.mapper.AnnouncementMapper;
import com.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final SysUserMapper sysUserMapper;

    /** 分页查询公告（按发布时间降序） */
    public IPage<Map<String, Object>> page(String status, long page, long size) {
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getPublishTime, Announcement::getCreateTime);
        if (status != null && !status.isEmpty()) {
            qw.eq(Announcement::getStatus, status);
        }
        IPage<Announcement> p = announcementMapper.selectPage(new Page<>(page, size), qw);

        // 补发布人姓名
        List<Long> userIds = p.getRecords().stream()
                .map(Announcement::getCreateUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        IPage<Map<String, Object>> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("content", a.getContent());
            m.put("priority", a.getPriority());
            m.put("status", a.getStatus());
            m.put("publishTime", a.getPublishTime());
            m.put("createTime", a.getCreateTime());
            m.put("createUserName", userNameMap.get(a.getCreateUserId()));
            return m;
        }).collect(Collectors.toList()));
        return result;
    }

    /** 最新 N 条已发布的公告（所有角色可见） */
    public List<Map<String, Object>> latest(int limit) {
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, "PUBLISHED")
                        .orderByDesc(Announcement::getPublishTime, Announcement::getCreateTime)
                        .last("LIMIT " + limit));
        return list.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("content", a.getContent());
            m.put("priority", a.getPriority());
            m.put("publishTime", a.getPublishTime());
            return m;
        }).collect(Collectors.toList());
    }

    public Long create(Long userId, String title, String content, String priority, String status) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setPriority(priority != null ? priority : "NORMAL");
        a.setStatus(status != null ? status : "PUBLISHED");
        a.setPublishTime("PUBLISHED".equals(a.getStatus()) ? LocalDateTime.now() : null);
        a.setCreateUserId(userId);
        announcementMapper.insert(a);
        return a.getId();
    }

    public void update(Long id, String title, String content, String priority, String status) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw BizException.notFound("公告不存在");
        a.setTitle(title);
        a.setContent(content);
        a.setPriority(priority);
        // 状态从草稿变为发布时设置发布时间
        if ("PUBLISHED".equals(status) && !"PUBLISHED".equals(a.getStatus())) {
            a.setPublishTime(LocalDateTime.now());
        }
        a.setStatus(status);
        announcementMapper.updateById(a);
    }

    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    public Announcement getById(Long id) {
        return announcementMapper.selectById(id);
    }
}