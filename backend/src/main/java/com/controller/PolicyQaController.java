package com.controller;

import com.common.CurrentUserUtil;
import com.common.R;
import com.dto.policy.PolicyAnswerResp;
import com.dto.policy.PolicyAskReq;
import com.service.PolicyQaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合规知识库 AI 问答（全部已登录角色可用）
 */
@Tag(name = "50.合规知识库 AI 问答", description = "基于 GLM 的实习管理办法问答助手")
@RestController
@RequestMapping("/policy")
@RequiredArgsConstructor
public class PolicyQaController {

    private final PolicyQaService policyQaService;

    @Operation(summary = "提问")
    @PostMapping("/ask")
    public R<PolicyAnswerResp> ask(@Valid @RequestBody PolicyAskReq req) {
        return R.ok(policyQaService.ask(CurrentUserUtil.userId(), req));
    }
}
