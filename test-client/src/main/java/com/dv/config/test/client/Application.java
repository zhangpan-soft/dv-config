package com.dv.config.test.client;

import com.dv.config.api.json.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper(){
        return JsonUtil.newObjectMapper();
    }
}
