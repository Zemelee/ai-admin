package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_announcement")
public class Announcement extends BaseEntity {
    private String title;
    private String content;
    private String priority;
    private String status;
    private LocalDateTime publishTime;
    private Long createUserId;
}