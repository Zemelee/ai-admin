package com.dto.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 政策问答请求体
 */
@Data
@Schema(description = "合规知识库 AI 问答")
public class PolicyAskReq {

    @Schema(description = "用户问题", example = "实习单位变更需要满足什么条件？")
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不超过 500 字")
    private String question;

    @Schema(description = "历史会话（最近 3 轮，user/assistant 交替，最多 6 条）")
    private List<HistoryMessage> history;

    @Data
    @Schema(description = "历史消息")
    public static class HistoryMessage {
        @Schema(description = "角色：user / assistant")
        private String role;
        @Schema(description = "内容")
        private String content;
    }
}
