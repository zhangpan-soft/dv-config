package com.dv.config.test.client.route;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class EnvironmentRouteDefinitionLocator implements RouteDefinitionLocator {

    private final Environment environment;

    public EnvironmentRouteDefinitionLocator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        BindResult<List<RouteDefinition>> bindResult = Binder.get(environment).bind("spring.cloud.gateway.routes", Bindable.listOf(RouteDefinition.class));
        return Flux.fromStream(bindResult.get().stream());
    }
}
