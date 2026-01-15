package com.dv.config.test.config;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteDefinitionLocator environmentRouteDefinitionLocator(Environment environment) {
        return () -> {
            BindResult<List<RouteDefinition>> bindResult = Binder.get(environment)
                    .bind("spring.cloud.gateway.routes", Bindable.listOf(RouteDefinition.class));
            if (bindResult.isBound()) {
                return Flux.fromIterable(bindResult.get());
            }
            return Flux.empty();
        };
    }
}
