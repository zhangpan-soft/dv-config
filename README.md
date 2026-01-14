# dv-config
轻量级配置中心

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

