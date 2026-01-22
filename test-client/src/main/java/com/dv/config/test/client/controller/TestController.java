package com.dv.config.test.client.controller;

import com.dv.config.api.dto.IResponse;
import com.dv.config.test.client.config.TestConfigure;
import com.dv.config.test.client.route.EnvironmentRouteDefinitionLocator;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RefreshScope
public class TestController {

    @Value("${test001:default}")
    private String test001;
    @Resource
    private EnvironmentRouteDefinitionLocator environmentRouteDefinitionLocator;

    @GetMapping("/config")
    public Mono<IResponse<?>> getConfig() {
        Map<String, Object> map = Map.of(
                "test001", test001,
                "obj", TestConfigure.getProperties() == null?"null":TestConfigure.getProperties()
        );
        return Mono
                .fromSupplier(()-> IResponse.ok(map))
                .onErrorResume(e-> Mono.fromSupplier(()->IResponse.fail(500, e.getMessage(), null)))
                .map(e-> e);
    }

    @GetMapping("/route")
    public Mono<IResponse<?>> getRoute(){
        return Mono.fromSupplier(()-> environmentRouteDefinitionLocator.getRouteDefinitions().collectList())
                .flatMap(list-> Mono.fromSupplier(()-> IResponse.ok(list)))
                .onErrorResume(e-> Mono.fromSupplier(()->IResponse.fail(500, e.getMessage(), null))).map(e->e);
    }
}
