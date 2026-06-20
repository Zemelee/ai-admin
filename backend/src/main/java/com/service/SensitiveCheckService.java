package com.service;

import cn.hutool.core.util.StrUtil;
import com.common.InternLogConst;
import com.dto.internlog.SensitiveResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志/周记敏感词（用工红线）检测服务，调用 GLM 识别风险关键词并生成高亮 HTML。
 * <p>检测维度对齐学院实习管理办法用工红线：
 * 休息日/法定节假日上班、夜班、强制加班、收取押金/培训费、禁入场所、安全事故、违规用工、薪酬异常等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveCheckService {

    private final GlmClient glmClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是实习合规风控助手。请审阅学生提交的实习日志/周记内容，识别其中是否涉及以下实习用工红线或风险情形：
            1. 休息日或法定节假日上班；
            2. 夜班；
            3. 强制加班、超时加班；
            4. 企业收取押金、培训费、服装费等费用；
            5. 实习单位属于禁入场所（酒吧、夜总会、歌厅、洗浴中心、电子游戏厅、网吧）；
            6. 安全事故、人身受伤；
            7. 其他违规用工或薪酬异常（如拖欠工资、低于最低工资）。
            仅当文本中明确出现上述情形的关键词或表述时才判定命中。只返回严格 JSON，不要任何额外文字：
            {"hit": true, "words": ["夜班","加班"]}，未命中返回 {"hit": false, "words": []}。
            words 为命中的风险关键词原文（不超过 8 个，每个不超过 20 字）。""";

    /**
     * 检测文本敏感词。GLM 调用失败时返回 empty（不阻塞业务提交）。
     *
     * @param content 日志/周记文本
     * @param bizId   关联业务单 ID（用于审计追溯）
     */
    public SensitiveResult check(String content, Long bizId, Long userId) {
        SensitiveResult result = SensitiveResult.empty();
        if (StrUtil.isBlank(content)) {
            return result;
        }
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sys = new LinkedHashMap<>();
            sys.put("role", "system");
            sys.put("content", SYSTEM_PROMPT);
            Map<String, String> user = new LinkedHashMap<>();
            user.put("role", "user");
            user.put("content", "请审阅：\n" + content);
            messages.add(sys);
            messages.add(user);

            String resp = glmClient.chat("SENSITIVE_DETECT", userId, InternLogConst.BIZ_LOG, bizId, messages);
            JsonNode node = parseJson(resp);
            if (node == null) {
                return result;
            }
            boolean hit = node.path("hit").asBoolean(false);
            List<String> words = new ArrayList<>();
            JsonNode wordsNode = node.path("words");
            if (wordsNode.isArray()) {
                for (JsonNode w : wordsNode) {
                    String s = w.asText("").trim();
                    if (!s.isEmpty() && words.size() < 8) {
                        words.add(s);
                    }
                }
            }
            result.setHit(hit || !words.isEmpty());
            result.setWords(String.join(",", words));
            result.setMarkedHtml(buildMarkedHtml(content, words));
        } catch (Exception e) {
            log.warn("敏感词检测失败（业务继续）: {}", e.getMessage());
        }
        return result;
    }

    private JsonNode parseJson(String resp) {
        if (StrUtil.isBlank(resp)) return null;
        try {
            return objectMapper.readTree(resp);
        } catch (Exception ignore) {
            // 模型可能偶发返回带 ```json 包裹，尝试提取
            Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(resp);
            if (m.find()) {
                try {
                    return objectMapper.readTree(m.group());
                } catch (Exception ignore2) {
                    return null;
                }
            }
            return null;
        }
    }

    /** 转义 HTML 后，将命中词包裹为 <span class="sensitive"> 高亮 */
    private String buildMarkedHtml(String content, List<String> words) {
        String escaped = escapeHtml(content);
        if (words == null || words.isEmpty()) {
            return escaped;
        }
        for (String w : words) {
            if (StrUtil.isBlank(w)) continue;
            String escWord = escapeHtml(w);
            Pattern p = Pattern.compile(Pattern.quote(escWord), Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(escaped);
            escaped = m.replaceAll("<span class=\"sensitive\">" + Matcher.quoteReplacement(escWord) + "</span>");
        }
        return escaped;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
