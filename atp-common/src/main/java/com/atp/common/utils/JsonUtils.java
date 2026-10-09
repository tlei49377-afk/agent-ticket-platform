package com.atp.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 统一的 JSON 工具。全局只维护一个 ObjectMapper 实例。
 */
public class JsonUtils {

    /**
     * 全局共享的 ObjectMapper 实例，统一 JSON 序列化/反序列化行为。
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            // 注册 Java 8 时间模块，支持 LocalDate、LocalDateTime、LocalTime 等新时间类型
            .registerModule(new JavaTimeModule())
            // 关闭时间戳输出，日期时间以 ISO-8601 字符串形式序列化（如 2026-10-09T10:00:00）
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // 反序列化时忽略 JSON 中的未知字段，避免对方新增字段导致解析失败
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtils(){

    }
    /**
     * 将 Java 对象序列化为 JSON 字符串。
     *
     * @param obj 待序列化的 Java 对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj){
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Java 对象。
     *
     * @param json  JSON 字符串
     * @param clazz 目标 Java 对象的类
     * @return 反序列化后的 Java 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz){
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 反序列化失败", e);
        }
    }
    /**
     * 将 JSON 字符串反序列化为 Java 对象。
     *
     * @param json  JSON 字符串
     * @param typeRef 目标 Java 对象的类型引用
     * @return 反序列化后的 Java 对象
     */

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 反序列化失败", e);
        }
    }
    /**
     * 获取全局共享的 ObjectMapper 实例。
     *
     * @return ObjectMapper 实例
     */

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
