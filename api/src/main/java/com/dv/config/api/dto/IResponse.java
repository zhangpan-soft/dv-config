package com.dv.config.api.dto;

import com.dv.config.api.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,  // 不显式添加 @type 字段
        property = "@type",
        defaultImpl = IResponse.Responses.class
) // 关键补丁
@JsonSubTypes(@JsonSubTypes.Type(value = IResponse.Responses.class))
public interface IResponse<DATA> {

    boolean getSuccess();

    String getMessage();

    DATA getData();

    long getTimestamp();

    Integer getCode();

    IResponse<DATA> setSuccess(boolean success);

    IResponse<DATA> setMessage(String message);

    IResponse<DATA> setTimestamp(long timestamp);

    IResponse<DATA> setData(DATA data);

    IResponse<DATA> setCode(Integer code);

    default String toJson(){
        return JsonUtil.toJson(this);
    };

    default byte[] toJsonBytes(){
        return JsonUtil.toBytes(this);
    }

    static <DATA> IResponse<DATA> fromJson(String json) {
        return JsonUtil.fromJson(json, new TypeReference<IResponse<DATA>>() {
        });
    }

    static <DATA> IResponse<DATA> fromJson(byte[] json) {
        return JsonUtil.fromJson(json, new TypeReference<IResponse<DATA>>() {
        });
    }

    static <DATA> IResponse<DATA> ok(DATA data, String message) {
        return new Responses<DATA>()
                .setSuccess(true)
                .setMessage(message)
                .setData(data)
                .setTimestamp(System.currentTimeMillis())
                .setCode(0);
    }

    static <DATA> IResponse<DATA> ok(DATA data) {
        return ok(data, null);
    }

    static <DATA> IResponse<DATA> ok() {
        return ok(null, null);
    }

    static <DATA> IResponse<DATA> fail(Integer code, String message, DATA data) {
        return new Responses<DATA>()
                .setSuccess(false)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis())
                .setData(data)
                .setCode(code);
    }

    static <DATA> IResponse<DATA> fail(Integer code, String message) {
        return fail(code, message, null);
    }

    static <DATA> IResponse<DATA> fail(Integer code) {
        return fail(code, null, null);
    }

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    class Responses<DATA> implements IResponse<DATA> {
        private boolean success;
        private String message;
        private DATA data;
        private long timestamp;
        private Integer code;

        @Override
        public boolean getSuccess() {
            return this.success;
        }
    }
}
