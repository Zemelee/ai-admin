package com.dto.internlog;

import lombok.Data;

/**
 * 日志/周记敏感词检测结果
 */
@Data
public class SensitiveResult {
    /** 是否命中 */
    private boolean hit;
    /** 命中词，逗号分隔 */
    private String words;
    /** 高亮 HTML 片段 */
    private String markedHtml;

    public static SensitiveResult empty() {
        SensitiveResult r = new SensitiveResult();
        r.hit = false;
        return r;
    }
}
