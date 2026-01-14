package com.dv.config.common;

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

public class JsonUtil {
    private JsonUtil(){
        throw new IllegalStateException("Utility class");
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, Boolean.FALSE);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, Boolean.TRUE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE);
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
    }

    @SneakyThrows
    public static String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @SneakyThrows
    public static byte[] toBytes(Object obj) {
        return objectMapper.writeValueAsBytes(obj);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    public static <T> T fromJson(byte[] bytes, Class<T> clazz) {
        return objectMapper.readValue(bytes, clazz);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        return objectMapper.readValue(json, typeReference);
    }

    @SneakyThrows
    public static <T> T fromJson(byte[] bytes, TypeReference<T> typeReference) {
        return objectMapper.readValue(bytes, typeReference);
    }
}
