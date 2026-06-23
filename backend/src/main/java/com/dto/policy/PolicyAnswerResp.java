package com.dto.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "政策问答响应")
public class PolicyAnswerResp {
    @Schema(description = "AI 答复")
    private String answer;
}
