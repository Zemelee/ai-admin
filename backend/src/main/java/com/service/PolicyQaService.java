package com.service;

import cn.hutool.core.util.StrUtil;
import com.common.PolicyConst;
import com.dto.policy.PolicyAnswerResp;
import com.dto.policy.PolicyAskReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合规知识库 AI 问答：基于内置 system prompt 调 GLM，无 RAG。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyQaService {

    private final GlmClient glmClient;

    /** 历史保留最多 6 条（3 轮），避免上下文过长 */
    private static final int MAX_HISTORY = 6;

    public PolicyAnswerResp ask(Long userId, PolicyAskReq req) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> sys = new LinkedHashMap<>();
        sys.put("role", "system");
        sys.put("content", PolicyConst.SYSTEM_PROMPT);
        messages.add(sys);

        // 历史（裁剪到最近 MAX_HISTORY 条，过滤非法 role）
        if (req.getHistory() != null && !req.getHistory().isEmpty()) {
            List<PolicyAskReq.HistoryMessage> hist = req.getHistory();
            int start = Math.max(0, hist.size() - MAX_HISTORY);
            for (int i = start; i < hist.size(); i++) {
                PolicyAskReq.HistoryMessage h = hist.get(i);
                if (h == null || StrUtil.isBlank(h.getContent())) continue;
                String role = h.getRole();
                if (!"user".equals(role) && !"assistant".equals(role)) continue;
                Map<String, String> m = new LinkedHashMap<>();
                m.put("role", role);
                m.put("content", h.getContent());
                messages.add(m);
            }
        }

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", req.getQuestion());
        messages.add(user);

        PolicyAnswerResp resp = new PolicyAnswerResp();
        try {
            String answer = glmClient.chat(PolicyConst.SCENE, userId, null, null, messages);
            resp.setAnswer(StrUtil.isBlank(answer) ? "AI 暂未给出答复，请稍后重试。" : answer);
        } catch (Exception e) {
            log.warn("政策问答失败 userId={} : {}", userId, e.getMessage());
            resp.setAnswer("AI 服务暂不可用，请稍后重试，或联系系部老师协助处理。");
        }
        return resp;
    }
}
