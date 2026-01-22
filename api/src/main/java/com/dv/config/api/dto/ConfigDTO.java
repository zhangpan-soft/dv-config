package com.dv.config.api.dto;

import lombok.Data;

import java.util.Map;
import java.util.TreeMap;

/**
 * 配置数据传输对象 (仅用于 Netty 传输)
 */
@Data
public class ConfigDTO {
    private Map<String, TreeMap<String, String>> configs = new TreeMap<>();
}
