package com.dv.config.api.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public enum JsonUtil {
    ;

    private static final ObjectMapper objectMapper = newObjectMapper();

    @SneakyThrows
    public static String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        return objectMapper.readValue(json, typeReference);
    }

    @SneakyThrows
    public static <T> T fromJson(byte[] json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    public static <T> T fromJson(byte[] json, TypeReference<T> typeReference) {
        return objectMapper.readValue(json, typeReference);
    }

    public static ObjectMapper newObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, Boolean.FALSE);
        // 针对传统时间处理
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 添加localdatetime支持
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.toInstant(ZoneOffset.ofHours(0)).toEpochMilli());
            }
        });
        javaTimeModule.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return new Date(Long.parseLong(p.getValueAsString())).toInstant().atOffset(ZoneOffset.ofHours(0)).toLocalDateTime();
            }
        });
        objectMapper.registerModule(javaTimeModule);
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, new JsonSerializer<>() {
            @Override
            public void serialize(Long aLong, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeString(aLong.toString());
            }
        });
        longModule.addDeserializer(Long.class, new JsonDeserializer<>() {
            @Override
            public Long deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                return Long.valueOf(jsonParser.getValueAsString());
            }
        });
        objectMapper.registerModule(longModule);
        SimpleModule bigDecimalModule = new SimpleModule();
        bigDecimalModule.addSerializer(BigDecimal.class, new JsonSerializer<>() {
            @Override
            public void serialize(BigDecimal aLong, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeString(aLong.stripTrailingZeros().toPlainString());
            }
        });
        bigDecimalModule.addDeserializer(BigDecimal.class, new JsonDeserializer<>() {

            @Override
            public BigDecimal deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                String value = jsonParser.getValueAsString();
                if (StringUtils.hasText(value)){
                    return new BigDecimal(value);
                }
                return null;
            }
        });
        objectMapper.registerModule(bigDecimalModule);
        return objectMapper;
    }

    @SneakyThrows
    public static byte[] toBytes(Object obj) {
        return objectMapper.writeValueAsBytes(obj);
    }
}
