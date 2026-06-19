package com.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson 全局配置：把 Long / long 序列化为字符串，避免 JS 端（Number 仅有 53 位精度）
 * 解析雪花 ID 时尾数被舍入。
 *
 * <p>影响范围：所有 Controller 返回体、嵌套对象、List/Map 中的 Long 字段都会输出为 JSON 字符串。
 * 反序列化（前端传 Long 字段过来）保持默认行为，Jackson 仍能把字符串/数字转为 Long。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("LongToString");
            JsonSerializer<Object> longAsString = new JsonSerializer<>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    if (value == null) {
                        gen.writeNull();
                    } else {
                        gen.writeString(value.toString());
                    }
                }
            };
            module.addSerializer(Long.class, longAsString);
            module.addSerializer(Long.TYPE, longAsString);
            builder.modulesToInstall(module);
        };
    }
}
