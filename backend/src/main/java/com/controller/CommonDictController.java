package com.controller;

import com.common.R;
import com.common.RoleEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公共字典接口（任意已登录角色可访问，纯枚举 / 配置）
 */
@Tag(name = "00.公共字典", description = "枚举/字典查询")
@RestController
@RequestMapping("/common/dict")
public class CommonDictController {

    @Operation(summary = "角色字典")
    @GetMapping("/roles")
    public R<List<Map<String, String>>> roles() {
        List<Map<String, String>> list = new ArrayList<>();
        for (RoleEnum r : RoleEnum.values()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("code", r.getCode());
            m.put("label", r.getLabel());
            list.add(m);
        }
        return R.ok(list);
    }

    @Operation(summary = "学生实习状态字典")
    @GetMapping("/intern-status")
    public R<List<Map<String, String>>> internStatus() {
        List<Map<String, String>> list = new ArrayList<>();
        String[][] dict = {
                {"ACTIVE", "实习中"},
                {"SUSPEND", "已暂停"},
                {"FINISHED", "已结束"},
                {"QUIT", "已退出"}
        };
        for (String[] kv : dict) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("code", kv[0]);
            m.put("label", kv[1]);
            list.add(m);
        }
        return R.ok(list);
    }
}
