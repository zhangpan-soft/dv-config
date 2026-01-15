package com.dv.config.test.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Value("${test.config:default}")
    private String testConfig;

    @GetMapping("/config")
    public String getConfig() {
        return "Current config value: " + testConfig;
    }
}
