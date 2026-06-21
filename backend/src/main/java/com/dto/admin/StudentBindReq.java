package com.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 学生绑定 teacher/mentor/company 请求体（任一非空即更新对应字段，null 表示不改动）
 */
@Data
@Schema(description = "学生绑定关系")
public class StudentBindReq {

    @Schema(description = "指导教师 teacher.id（null 不改动，0 表示解绑）")
    private Long teacherId;

    @Schema(description = "实习企业 company.id（null 不改动，0 表示解绑）")
    private Long companyId;

    @Schema(description = "企业指导 mentor.id（null 不改动，0 表示解绑）")
    private Long mentorId;
}
