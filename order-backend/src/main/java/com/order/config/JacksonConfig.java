package com.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 全局 Jackson 时间格式配置
 *
 * spring.jackson.date-format 仅对 java.util.Date 生效，
 * 对 LocalDateTime / LocalDate 无效（默认会序列化成带 T 的 ISO 字符串）。
 * 这里统一注册 JavaTimeModule，让 LocalDateTime 输出 "yyyy-MM-dd HH:mm:ss"、
 * LocalDate 输出 "yyyy-MM-dd"，与前端展示保持一致。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            JavaTimeModule module = new JavaTimeModule();
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME));
            module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE));
            builder.modules(module);
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }

    /** 提供给非 Spring 管理场景下使用（如工具类） */
    @Bean
    public ObjectMapper objectMapper(List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        for (Jackson2ObjectMapperBuilderCustomizer customizer : customizers) {
            customizer.customize(builder);
        }
        return builder.build();
    }
}
