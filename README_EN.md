<p align="center">
  <a href="./README.md">简体中文</a> | <b>English</b>
</p>

# dv-config
**Lightweight Configuration Center**

Designed to solve the problem of managing configurations directly in backend management systems without introducing heavy configuration middleware.
In real-world projects, there are often many dynamic configurations that need to be adjusted by operations or product managers. The core purpose of this project is to solve this problem in a unified and lightweight way.

# Modules
*   `api`: Core interface definitions, DTOs, and common utilities.
*   `server`: Netty server core logic.
*   `client`: Netty client core logic.
*   `server-impl`: Default database-based server implementation (includes Admin UI).
*   `test-server`: Server test application.
*   `test-client`: Client test application.

# Quick Start

### Server Usage
1. Import the `server` module.
2. Import the `server-impl` module (or implement your own `api` interface).
3. Configure Database and Redis.

### Client Usage
1. Import the `client` module.
2. Configure Netty connection information.

# Configuration Details

### 1. SERVER Configuration (`server` module)
```yaml
netty:
  server:
    port: ${NETTY_PORT:8888} # Netty server port
    boss-thread-count: ${NETTY_BOSS_THREAD_COUNT:1} # Boss thread count
    worker-thread-count: ${NETTY_WORKER_THREAD_COUNT:4} # Worker thread count
    idle-timeout: 65 # Client heartbeat timeout (seconds)
  # Encryption Configuration
  crypto:
    enabled: ${CRYPTO_ENABLED:false} # Enable encryption
    master-key: ${CRYPTO_MASTER_KEY:}  # Master key (Required in production)
    current-version: v1 # Current key version
    algorithm: AES-256-GCM # Encryption algorithm
    iterations: 65536 # Encryption iterations
```

### 2. SERVER IMPL Configuration (`server-impl` module)
If using the default database implementation, configure the following:

```yaml
spring:
  liquibase:
    enabled: true
    default-schema: ${DB_SCHEMA:config-server}
    change-log: classpath*:db/changelog/master.xml
    parameters:
      TABLE_NAME_CONFIG: ${TABLE_NAME_CONFIG:sys_config}
      TABLE_NAME_ROUTE: ${TABLE_NAME_ROUTE:sys_route}
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${spring.liquibase.default-schema}?${DB_URL_PARAMS:useUnicode=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF8&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 1
      maximum-pool-size: 10
  data:
    redis:
      database: ${REDIS_DATABASE:7}
      host: ${REDIS_HOST:localhost}
      password: ${REDIS_PASSWORD:}
      port: ${REDIS_PORT:6379}

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      column-format: "`%s`"
      insert-strategy: not_null
      update-strategy: not_null
  mapper-locations: classpath*:/mapper/**/*.xml

# Admin UI Configuration
config:
  admin:
    ui:
      enabled: false # Enable built-in admin UI
      public-path: "" # Frontend public path, default empty
```

### 3. CLIENT Configuration (`client` module)
```yaml
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
  # Encryption Configuration (Must match server)
  crypto:
    enabled: false 
    master-key: ${CRYPTO_MASTER_KEY:}
    current-version: v1
    algorithm: AES-256-GCM
    iterations: 65536
```

# Environment Variables
Supported environment variables for containerized deployment:

| Variable | Default | Description |
| :--- | :--- | :--- |
| `NETTY_PORT` | 8888 | Netty server port |
| `NETTY_BOSS_THREAD_COUNT` | 1 | Netty Boss thread count |
| `NETTY_WORKER_THREAD_COUNT` | 4 | Netty Worker thread count |
| `CRYPTO_ENABLED` | false | Enable encryption |
| `CRYPTO_MASTER_KEY` | (empty) | Encryption master key |
| `DB_HOST` | localhost | Database host |
| `DB_PORT` | 3306 | Database port |
| `DB_SCHEMA` | config-server | Database schema name |
| `DB_USERNAME` | root | Database username |
| `DB_PASSWORD` | 123456 | Database password |
| `DB_URL_PARAMS` | (see config) | JDBC URL parameters |
| `TABLE_NAME_CONFIG` | sys_config | Config table name |
| `TABLE_NAME_ROUTE` | sys_route | Route table name |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `REDIS_PASSWORD` | (empty) | Redis password |
| `REDIS_DATABASE` | 7 | Redis database index |

# Admin UI Features
The `server-impl` module provides a comprehensive lightweight management interface (Vue 3 + Element Plus SPA), supporting:

1.  **Config Management**:
    *   CRUD, fuzzy search.
    *   **Draft Mechanism**: Changes are saved as drafts first, published after review.
    *   **History**: Automatically keeps the last 10 versions, supports one-click rollback.
    *   **Batch Operations**: Batch add, delete, enable, disable.
    *   **Encryption Tool**: Online encryption for sensitive values.

2.  **Route Management**:
    *   Supports Spring Cloud Gateway dynamic routing.
    *   JSON editor for Predicates and Filters.
    *   Supports draft, publish, history, and rollback workflows.

3.  **Access URLs**:
    *   Config: `/admin/config`
    *   Route: `/admin/route`

# Important Notes
1.  The encryption algorithm only supports **AES-256-GCM**.
2.  The Client decrypts data only after receiving it from the Server. The Server stores and transmits data as-is.
3.  When decrypting, if the configuration value format is not a valid encrypted JSON string, the Client treats it as plain text.

# Usage Examples

### 1. Client Dynamic Configuration Listener
```java
@Slf4j
@Component
public class ConfigRefreshListener implements ApplicationListener<ConfigRefreshEvent> {

    @Autowired(required = false)
    private ContextRefresher contextRefresher;

    @Override
    public void onApplicationEvent(@Nonnull ConfigRefreshEvent event) {
        if (contextRefresher != null) {
            contextRefresher.refresh();
            log.info("Configuration refreshed successfully");
        }
    }
}
```

### 2. Client Dynamic Route Listener (Gateway)
```java
@Slf4j
@Component
public class RouteRefreshListener implements ApplicationListener<RouteRefreshEvent> {

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(@Nonnull RouteRefreshEvent event) {
        applicationContext.publishEvent(new RefreshRoutesEvent(this));
    }
}
```
