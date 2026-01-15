<p align="center">
  <b>简体中文</b> | <a href="./README_EN.md">English</a>
</p>

# dv-config
轻量级配置中心

为了解决配置无法在后台管理系统直接管理的问题，以及避免引入过重的配置中间件。
在实际项目中，往往有很多动态配置需要运营或者产品去动态调整，核心目的就是为了统一解决此问题。

# 服务端使用
1. 任意 Spring Boot 项目，引入 `server` 模块。
2. 引入你自己的 `api` 模块实现，或者直接使用默认的数据库实现 `default-api-impl` 模块。

# 客户端使用
1. 任意 Spring Boot 项目，引入 `client` 模块。

# 配置说明

### SERVER 端配置项
```yaml
netty:
  server:
    port: ${NETTY_PORT:8888} # Netty服务端口
    boss-thread-count: ${NETTY_BOSS_THREAD_COUNT:1} # Boss线程数
    worker-thread-count: ${NETTY_WORKER_THREAD_COUNT:4} # Worker线程数
    idle-timeout: 65 # 客户端心跳超时时间(秒)
  # 加密配置
  crypto:
    enabled: false # 是否启用
    master-key: ${CRYPTO_MASTER_KEY:}  # 加密的主key
    current-version: v1 # 当前使用的版本
    algorithm: AES-256-GCM # 加密算法
    iterations: 65536 # 加密迭代次数
```

### CLIENT 端配置项
```yaml
# Netty客户端配置
netty:
  client:
    host: localhost # Netty服务端的地址
    port: 8888 # Netty服务端的端口
    # 需要订阅的命名空间
    subscribe-namespaces:
      - ${spring.application.name}
      - common
      - exception
      - messages
    max-attempts: 10 # 重连最大次数
    initial-delay: 15000 # 初始重连间隔(ms)
    max-delay: 300000 # 最大重连间隔(ms)
    has-route: true # 是否启用路由订阅 (Gateway需开启)
    config-poll-interval: 60000 # 配置轮询间隔(ms)
    route-poll-interval: 60000 # 路由轮询间隔(ms)
    
    # 需要刷新的命名空间
    # 注意: 此处订阅刷新后, 会发送配置刷新事件 `com.dv.config.client.event.ConfigRefreshEvent` 
    # 或路由刷新事件 `com.dv.config.client.event.RouteRefreshEvent`
    # 无论是否有订阅刷新的命名空间, 当订阅的命名空间有配置改变, 都会刷新 Spring Boot 的当前环境变量(Environment)
    refresh-namespaces:
      - casino-gateway
      - common
  # 加密配置
  crypto:
    enabled: false # 是否启用
    master-key: ${CRYPTO_MASTER_KEY:}  # 加密的主key
    current-version: v1 # 当前使用的版本
    algorithm: AES-256-GCM # 加密算法
    iterations: 65536 # 加密迭代次数
```

### default-api-impl 配置 (基于数据库的默认实现)
如果使用了 `default-api-impl` 模块，需要配置数据库相关信息：
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      column-format: "`%s`"  # 给所有字段加上反引号避免保留字
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

# 启用管理 UI (可选)
dv:
  config:
    admin-ui:
      enabled: true # 开启内置管理界面
```

# 管理 UI 功能
`default-api-impl` 模块提供了一个轻量级的管理界面，支持：
1. **配置管理**: 增删改查、草稿机制、历史记录、一键回滚、批量操作（启用/禁用/删除）。
2. **路由管理**: 动态路由配置、JSON 编辑、草稿与发布流程、历史记录与回滚。
3. **差异对比**: 发布前对比草稿与线上配置的差异。
4. **权限集成**: 通过实现 `UserProvider` 接口对接自定义用户系统。

访问地址:
- Config: `/dv-config/admin/config`
- Route: `/dv-config/admin/route`

# 注意事项
1. 加密算法只提供 256位 AES-GCM。
2. Client 接到 Server 端数据才会解密，Server 端数据存储和传输时保持原样（如果是加密存储则传输加密值）。
3. Client 在解密时，如果配置值格式不是加密后的 JSON 字符串，会按明文处理。

# 其他说明
1. 配置的加载会在 Spring Boot 服务启动前就开始加载，因此像数据库等信息可以使用 Client 加载。
2. 如果需要使用 Spring Cloud 的配置刷新机制，请监听配置刷新事件，并配合 Spring Cloud 刷新机制（`ContextRefresher`），路由同理。
3. 在实现 API 时，如果需要刷新配置或路由，请在配置或路由变更时，触发刷新：
   - `com.dv.config.server.handler.NettyConfigHandler#refresh`
   - `com.dv.config.server.handler.NettyRouteHandler#refresh`

# 使用示例

### 1. 客户端动态配置监听
```java
/**
 * 配置刷新监听器
 * 监听配置更新事件，触发 Spring Cloud Context 刷新
 */
@Slf4j
@Component
public class ConfigRefreshListener implements ApplicationListener<ConfigRefreshEvent> {

    @Autowired(required = false)
    private ContextRefresher contextRefresher;

    @Override
    public void onApplicationEvent(@Nonnull ConfigRefreshEvent event) {
        // 如果有 ContextRefresher（Spring Cloud Context），触发刷新
        // 这会刷新所有 @RefreshScope 的 Bean 和 @ConfigurationProperties
        if (contextRefresher != null) {
            try {
                contextRefresher.refresh();
                log.info("配置刷新成功");
            } catch (Exception e) {
                log.error("配置刷新失败", e);
            }
        } else {
            if (log.isDebugEnabled())
                log.debug("ContextRefresher 未启用，跳过 @RefreshScope Bean 刷新");
        }
    }
}
```

### 2. 客户端动态路由监听 (Gateway)
```java
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 路由刷新监听器
 * 监听来自 config-server 的路由更新事件
 */
@Slf4j
@Component
public class RouteRefreshListener implements ApplicationListener<RouteRefreshEvent> {

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(@Nonnull RouteRefreshEvent event) {
        log.info("接收到路由更新事件");
        try {
            applicationContext.publishEvent(new RefreshRoutesEvent(this));
        } catch (Exception e) {
            log.error("处理路由更新事件失败", e);
        }
    }
}
```

### 3. 路由定义加载器 (Gateway)
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
        // 从 Environment 中读取 spring.cloud.gateway.routes 配置
        BindResult<List<RouteDefinition>> bindResult = Binder.get(environment)
                .bind("spring.cloud.gateway.routes", Bindable.listOf(RouteDefinition.class));
        return Flux.fromStream(bindResult.get().stream());
    }
}
```