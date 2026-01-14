# dv-config
轻量级配置中心
为了解决配置无法再后台管理系统直接管理问题,以及需要引入配置中间件问题.
在实际项目中,往往有很多动态配置需要运营或者产品去动态调整的,核心目的就是为了解决此统一问题.

# 服务端使用
1. 随便一个springboot项目, 引入`server`模块, 引入你自己的 `api` 模块的实现即可

# 客户端使用
1. 随便一个springboot项目, 引入`client`模块

# 配置
### SERVER 端配置项
```yaml
netty:
  server:
    port: ${NETTY_PORT:8888} # netty服务端口
    boss-thread-count: ${NETTY_BOSS_THREAD_COUNT:1} # netty服务线程数
    worker-thread-count: ${NETTY_WORKER_THREAD_COUNT:4} # netty服务工作线程数
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
    host: localhost # netty服务端的地址
    port: 8888 # netty服务端的端口
    # 需要订阅的命名空间
    subscribe-namespaces:
      - ${spring.application.name}
      - common
      - exception
      - messages
    max-attempts: 10 # 重连最大次数
    initial-delay: 15000 # 初始重连间隔
    max-delay: 300000 # 最大重连间隔
    has-route: true # 是否启用路由订阅
    # 需要刷新的命名空间, 注意: 此处订阅刷新后, 会发送配置刷新的事件`com.dv.config.client.event.ConfigRefreshEvent` 或路由舒心事件`com.dv.config.client.event.RouteRefreshEvent`
    # 无论是否有订阅刷新的命名空间, 当订阅的命名空间有配置改变, 都会刷新springboot的当前环境变量(Environment), 使用Environment#getProperty 会得到最新配置
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

### default-api-impl 配置
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
```
# 注意事项
1. 加密算法只提供256位AES-GCM
2. client接到server端数据才会解密, server端数据不会解密
3. client在解密时, 如果格式值格式不是加密后的json字符串, 会按明文处理

# 其他说明
1. 配置的加载会在springboot服务启动前就开始加载, 因此像数据库等信息可以使用client加载
2. 如果需要使用spring cloud的配置刷新机制,请监听配置刷新事件, 并配合 spring cloud刷新机制, 路由同理
3. 在实现 api 时, 如果需要刷新配置或路由, 请在配置或路由变更时, 触发刷新 `com.dv.config.server.handler.NettyConfigHandler#refresh`或`com.dv.config.server.handler.NettyRouteHandler#refresh`


### 客户端动态配置
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

### 客户端动态路由
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
        BindResult<List<RouteDefinition>> bindResult = Binder.get(environment).bind("spring.cloud.gateway.routes", Bindable.listOf(RouteDefinition.class));
        return Flux.fromStream(bindResult.get().stream());
    }
}

```