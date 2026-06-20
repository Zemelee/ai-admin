package com.service;

import com.common.BizException;
import com.config.GlmConfig;
import com.entity.AiCallLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GLM-4-Air 统一调用客户端。
 * <p>所有系统内语义识别、政策问答、自动文本生成均经此调用，统一鉴权并落 ai_call_log 审计。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlmClient {

    private final GlmConfig glmConfig;
    @Qualifier("glmRestTemplate")
    private final RestTemplate glmRestTemplate;
    private final ObjectMapper objectMapper;
    private final AiCallLogMapper aiCallLogMapper;

    /**
     * 发起一次 chat completions 调用，返回模型输出文本。
     *
     * @param scene    场景：CHAT_QA / SENSITIVE_DETECT ...
     * @param userId   调用人（系统调用传 null）
     * @param bizType  关联业务类型
     * @param bizId    关联业务单 ID
     * @param messages OpenAI 风格消息列表 [{role, content}]
     * @return 模型输出文本
     */
    public String chat(String scene, Long userId, String bizType, Long bizId,
                       List<Map<String, String>> messages) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", glmConfig.getModel());
        payload.put("messages", messages);
        payload.put("temperature", glmConfig.getTemperature());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(glmConfig.getApiKey());

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BizException("GLM 请求序列化失败");
        }

        long start = System.currentTimeMillis();
        AiCallLog rec = new AiCallLog();
        rec.setScene(scene);
        rec.setUserId(userId);
        rec.setModel(glmConfig.getModel());
        rec.setRequestPayload(requestJson);
        rec.setBizType(bizType);
        rec.setBizId(bizId);

        try {
            String resp = glmRestTemplate.postForObject(
                    glmConfig.getEndpoint(),
                    new HttpEntity<>(payload, headers),
                    String.class);
            rec.setResponsePayload(resp);
            rec.setSuccess(1);

            JsonNode root = objectMapper.readTree(resp);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText("");
                JsonNode usage = root.path("usage");
                rec.setPromptTokens(usage.path("prompt_tokens").asInt(0));
                rec.setCompletionTokens(usage.path("completion_tokens").asInt(0));
                rec.setTotalTokens(usage.path("total_tokens").asInt(0));
                rec.setCostMs((int) (System.currentTimeMillis() - start));
                aiCallLogMapper.insert(rec);
                return content;
            }
            throw new BizException("GLM 返回为空");
        } catch (RestClientException e) {
            log.error("GLM 调用失败 scene={}", scene, e);
            rec.setSuccess(0);
            rec.setErrorMsg(truncate(e.getMessage()));
            rec.setCostMs((int) (System.currentTimeMillis() - start));
            aiCallLogMapper.insert(rec);
            throw new BizException("AI 服务调用失败，请稍后重试");
        } catch (BizException e) {
            rec.setSuccess(0);
            rec.setErrorMsg(truncate(e.getMessage()));
            rec.setCostMs((int) (System.currentTimeMillis() - start));
            aiCallLogMapper.insert(rec);
            throw e;
        } catch (Exception e) {
            log.error("GLM 响应解析失败 scene={}", scene, e);
            rec.setSuccess(0);
            rec.setErrorMsg(truncate(e.getMessage()));
            rec.setCostMs((int) (System.currentTimeMillis() - start));
            aiCallLogMapper.insert(rec);
            throw new BizException("AI 响应解析失败");
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
