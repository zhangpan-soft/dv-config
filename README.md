<p align="center">
  <b>简体中文</b> | <a href="./README_EN.md">English</a>
</p>

# dv-config
轻量级配置中心

为了解决配置无法在后台管理系统直接管理的问题，以及避免引入过重的配置中间件。
在实际项目中，往往有很多动态配置需要运营或者产品去动态调整，核心目的就是为了统一解决此问题。

# 模块说明
*   `api`: 核心接口定义、DTO、公共工具类。
*   `server`: Netty 服务端核心逻辑。
*   `client`: Netty 客户端核心逻辑。
*   `server-impl`: 基于数据库的默认服务端实现（包含管理 UI）。
*   `test-server`: 服务端测试应用。
*   `test-client`: 客户端测试应用。

# 快速开始

### 服务端使用
1. 引入 `server` 模块。
2. 引入 `server-impl` 模块（或者实现你自己的 `api` 接口）。
3. 配置数据库和 Redis。

### 客户端使用
1. 引入 `client` 模块。
2. 配置 Netty 连接信息。

# 配置详解

### 1. SERVER 端配置 (`server` 模块)
```yaml
netty:
  server:
    port: ${NETTY_PORT:8888} # Netty服务端口
    boss-thread-count: ${NETTY_BOSS_THREAD_COUNT:1} # Boss线程数
    worker-thread-count: ${NETTY_WORKER_THREAD_COUNT:4} # Worker线程数
    # 加密配置
  crypto:
    enabled: ${CRYPTO_ENABLED:false} # 是否启用加密
    master-key: ${CRYPTO_MASTER_KEY:}  # 加密的主key (生产环境必须设置)
    current-version: v1 # 当前使用的版本
    algorithm: AES-256-GCM # 加密算法
    iterations: 65536 # 加密迭代次数
```

### 2. SERVER IMPL 端配置 (`server-impl` 模块)
如果使用了默认的数据库实现，需要配置以下信息：

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

# 管理 UI 配置
config:
  admin:
    ui:
      enabled: false # 是否启用内置管理界面
      public-path: "" # 前端资源公共路径，默认为空
```

### 3. CLIENT 端配置 (`client` 模块)
```yaml
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
  # 加密配置 (需与服务端保持一致)
  crypto:
    enabled: false 
    master-key: ${CRYPTO_MASTER_KEY:}
    current-version: v1
    algorithm: AES-256-GCM
    iterations: 65536
```

# 环境变量清单
为了方便容器化部署，项目支持以下环境变量配置：

| 变量名 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `NETTY_PORT` | 8888 | Netty 服务端口 |
| `NETTY_BOSS_THREAD_COUNT` | 1 | Netty Boss 线程数 |
| `NETTY_WORKER_THREAD_COUNT` | 4 | Netty Worker 线程数 |
| `CRYPTO_ENABLED` | false | 是否启用加密 |
| `CRYPTO_MASTER_KEY` | (空) | 加密主密钥 (启用加密时必填) |
| `DB_HOST` | localhost | 数据库地址 |
| `DB_PORT` | 3306 | 数据库端口 |
| `DB_SCHEMA` | config-server | 数据库 Schema 名称 |
| `DB_USERNAME` | root | 数据库用户名 |
| `DB_PASSWORD` | 123456 | 数据库密码 |
| `DB_URL_PARAMS` | (见配置) | JDBC URL 参数 |
| `TABLE_NAME_CONFIG` | sys_config | 配置表名 |
| `TABLE_NAME_ROUTE` | sys_route | 路由表名 |
| `REDIS_HOST` | localhost | Redis 地址 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `REDIS_DATABASE` | 7 | Redis 数据库索引 |

# 管理 UI 功能
`server-impl` 模块提供了一个功能完善的轻量级管理界面 (Vue 3 + Element Plus SPA)，支持：

1.  **配置管理**:
    *   增删改查、模糊搜索。
    *   **草稿机制**: 修改先保存为草稿，确认无误后发布。
    *   **历史记录**: 自动保留最近 10 个版本，支持一键回滚。
    *   **批量操作**: 批量添加、删除、启用、禁用。
    *   **加密工具**: 提供在线加密功能，敏感配置可加密保存。

2.  **路由管理**:
    *   支持 Spring Cloud Gateway 动态路由配置。
    *   提供 JSON 编辑器配置 Predicates 和 Filters。
    *   同样支持草稿、发布、历史和回滚流程。

3.  **访问地址**:
    *   配置管理: `/admin/config`
    *   路由管理: `/admin/route`

# 注意事项
1.  加密算法只提供 256位 AES-GCM。
2.  Client 接到 Server 端数据才会解密，Server 端数据存储和传输时保持原样（如果是加密存储则传输加密值）。
3.  Client 在解密时，如果配置值格式不是加密后的 JSON 字符串，会按明文处理。

# 使用示例

### 1. 客户端动态配置监听
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
            log.info("配置刷新成功");
        }
    }
}
```

### 2. 客户端动态路由监听 (Gateway)
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
