# dv-config
**Lightweight Configuration Center**

Designed to solve the problem of managing configurations directly in backend management systems without introducing heavy configuration middleware.
In real-world projects, there are often many dynamic configurations that need to be adjusted by operations or product managers. The core purpose of this project is to solve this problem in a unified and lightweight way.

# Server Usage
1. Import the `server` module into any Spring Boot project.
2. Import your own implementation of the `api` module, or directly use the default database-based implementation module: `default-api-impl`.

# Client Usage
1. Import the `client` module into any Spring Boot project.

# Configuration

### SERVER Configuration
```yaml
netty:
  server:
    port: ${NETTY_PORT:8888} # Netty server port
    boss-thread-count: ${NETTY_BOSS_THREAD_COUNT:1} # Boss thread count
    worker-thread-count: ${NETTY_WORKER_THREAD_COUNT:4} # Worker thread count
    idle-timeout: 65 # Client heartbeat timeout (seconds)
  # Encryption Configuration
  crypto:
    enabled: false # Enable encryption
    master-key: ${CRYPTO_MASTER_KEY:}  # Master key for encryption
    current-version: v1 # Current key version
    algorithm: AES-256-GCM # Encryption algorithm
    iterations: 65536 # Encryption iterations
```

### CLIENT Configuration
```yaml
# Netty Client Configuration
netty:
  client:
    host: localhost # Netty server address
    port: 8888 # Netty server port
    # Namespaces to subscribe to
    subscribe-namespaces:
      - ${spring.application.name}
      - common
      - exception
      - messages
    max-attempts: 10 # Maximum reconnection attempts
    initial-delay: 15000 # Initial reconnection delay (ms)
    max-delay: 300000 # Maximum reconnection delay (ms)
    has-route: true # Enable route subscription (Required for Gateway)
    config-poll-interval: 60000 # Configuration polling interval (ms)
    route-poll-interval: 60000 # Route polling interval (ms)
    
    # Namespaces that trigger refresh events
    # Note: When these namespaces are refreshed, a `com.dv.config.client.event.ConfigRefreshEvent` 
    # or `com.dv.config.client.event.RouteRefreshEvent` will be published.
    # Regardless of whether a namespace is in this list, changes to subscribed namespaces 
    # will always refresh the current Spring Boot Environment.
    refresh-namespaces:
      - casino-gateway
      - common
  # Encryption Configuration
  crypto:
    enabled: false # Enable encryption
    master-key: ${CRYPTO_MASTER_KEY:}  # Master key for encryption
    current-version: v1 # Current key version
    algorithm: AES-256-GCM # Encryption algorithm
    iterations: 65536 # Encryption iterations
```

### default-api-impl Configuration (Default Database Implementation)
If you use the `default-api-impl` module, you need to configure the database connection:
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      column-format: "`%s`"  # Add backticks to all fields to avoid reserved words
      insert-strategy: not_null
      update-strategy: not_null
  mapper-locations: classpath*:/mapper/**/*.xml

spring:
  liquibase:
    enabled: true
    default-schema: ${DATABASE_SCHEMA:config-server}
    change-log: db/changelog/master.xml
    parameters:
      TABLE_NAME_CONFIG: ${TABLE_NAME_CONFIG:config}
      TABLE_NAME_ROUTE: ${TABLE_NAME_ROUTE:route}
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 1
      maximum-pool-size: 10
```

# Important Notes
1. The encryption algorithm only supports **AES-256-GCM**.
2. The Client decrypts data only after receiving it from the Server. The Server stores and transmits data as-is (if stored encrypted, it transmits encrypted values).
3. When decrypting, if the configuration value format is not a valid encrypted JSON string, the Client treats it as plain text.

# Additional Information
1. Configuration loading starts **before** the Spring Boot service fully starts, so information like database credentials can be loaded using the client.
2. To use the Spring Cloud configuration refresh mechanism, please listen for the configuration refresh event and cooperate with the Spring Cloud refresh mechanism (`ContextRefresher`). The same applies to routes.
3. When implementing the API, if you need to refresh configurations or routes, please trigger the refresh when changes occur:
   - `com.dv.config.server.handler.NettyConfigHandler#refresh`
   - `com.dv.config.server.handler.NettyRouteHandler#refresh`

# Usage Examples

### 1. Client Dynamic Configuration Listener
```java
/**
 * Configuration Refresh Listener
 * Listens for config update events and triggers Spring Cloud Context refresh.
 */
@Slf4j
@Component
public class ConfigRefreshListener implements ApplicationListener<ConfigRefreshEvent> {

    @Autowired(required = false)
    private ContextRefresher contextRefresher;

    @Override
    public void onApplicationEvent(@Nonnull ConfigRefreshEvent event) {
        // If ContextRefresher (Spring Cloud Context) is present, trigger refresh.
        // This refreshes all @RefreshScope Beans and @ConfigurationProperties.
        if (contextRefresher != null) {
            try {
                contextRefresher.refresh();
                log.info("Configuration refreshed successfully");
            } catch (Exception e) {
                log.error("Configuration refresh failed", e);
            }
        } else {
            if (log.isDebugEnabled())
                log.debug("ContextRefresher not enabled, skipping @RefreshScope Bean refresh");
        }
    }
}
```

### 2. Client Dynamic Route Listener (Gateway)
```java
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Route Refresh Listener
 * Listens for route update events from config-server.
 */
@Slf4j
@Component
public class RouteRefreshListener implements ApplicationListener<RouteRefreshEvent> {

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(@Nonnull RouteRefreshEvent event) {
        log.info("Received route update event");
        try {
            applicationContext.publishEvent(new RefreshRoutesEvent(this));
        } catch (Exception e) {
            log.error("Failed to handle route update event", e);
        }
    }
}
```

### 3. Route Definition Locator (Gateway)
```java
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
        // Bind spring.cloud.gateway.routes configuration from Environment
        BindResult<List<RouteDefinition>> bindResult = Binder.get(environment)
                .bind("spring.cloud.gateway.routes", Bindable.listOf(RouteDefinition.class));
        return Flux.fromStream(bindResult.get().stream());
    }
}
```