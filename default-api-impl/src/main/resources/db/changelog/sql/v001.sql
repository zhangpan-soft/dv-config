-- liquibase formatted sql

-- changeset config:001 endDelimiter:;

create table `${TABLE_NAME_CONFIG}`
(
    `id`          bigint       not null comment '主键' auto_increment,
    `key`         varchar(255) not null comment 'key',
    `namespace`   varchar(255) not null comment '命名空间' DEFAULT 'DEFAULT',
    `value`       text         not null comment 'value',
    `description` varchar(500) null comment '描述',
    `enabled`     tinyint      not null comment '是否启用' default 0,
    `encrypted`     tinyint      not null comment '是否加密' default 0,
    `create_time` datetime     not null comment '创建时间' default current_timestamp,
    `update_time` datetime     not null comment '更新时间' default current_timestamp on update current_timestamp,
    primary key (`id`),
    unique key `uk_namespace_key` (`namespace`, `key`)
)default character set utf8mb4 comment='配置表';

-- 创建网关路由表
CREATE TABLE `${TABLE_NAME_ROUTE}`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_id`    varchar(100) NOT NULL COMMENT '路由ID',
    `uri`         varchar(500) NOT NULL COMMENT '目标URI',
    `predicates`  text COMMENT '断言配置(JSON数组)',
    `filters`     text COMMENT '过滤器配置(JSON数组)',
    `metadata`    text COMMENT '元数据(JSON对象)',
    `order_num`   int                   DEFAULT 0 COMMENT '排序',
    `enabled`     tinyint(1)            DEFAULT 1 COMMENT '是否启用',
    `description` varchar(500) COMMENT '描述',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_id` (`route_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='网关路由配置表';