-- liquibase formatted sql

-- changeset config:002 endDelimiter:;

-- 1. Config 表添加审计字段
ALTER TABLE `${TABLE_NAME_CONFIG}`
    ADD COLUMN `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
    ADD COLUMN `update_by` varchar(100) DEFAULT NULL COMMENT '更新人';

-- 2. Route 表结构调整：删除自增ID，将 route_id 改为 id 并设为主键，添加审计字段
-- 注意：这会删除原有的 id 列，请确保数据备份
ALTER TABLE `${TABLE_NAME_ROUTE}` DROP COLUMN `id`;
ALTER TABLE `${TABLE_NAME_ROUTE}` CHANGE COLUMN `route_id` `id` varchar(100) NOT NULL COMMENT '路由ID';
ALTER TABLE `${TABLE_NAME_ROUTE}` DROP INDEX `uk_route_id`;
ALTER TABLE `${TABLE_NAME_ROUTE}` ADD PRIMARY KEY (`id`);
ALTER TABLE `${TABLE_NAME_ROUTE}`
    ADD COLUMN `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
    ADD COLUMN `update_by` varchar(100) DEFAULT NULL COMMENT '更新人';

-- 3. 创建 Config Draft 表
CREATE TABLE `${TABLE_NAME_CONFIG}_draft` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '草稿ID',
    `config_id` bigint DEFAULT NULL COMMENT '关联的正式配置ID(如果是修改)',
    `namespace` varchar(255) NOT NULL COMMENT '命名空间',
    `key` varchar(255) NOT NULL COMMENT 'key',
    `value` text NOT NULL COMMENT 'value',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用',
    `encrypted` tinyint NOT NULL DEFAULT 0 COMMENT '是否加密',
    `operation_type` varchar(20) NOT NULL COMMENT '操作类型: ADD, UPDATE, DELETE',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` varchar(100) DEFAULT NULL,
    `update_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_draft_namespace_key` (`namespace`, `key`)
) DEFAULT CHARSET=utf8mb4 COMMENT='配置草稿表';

-- 4. 创建 Config History 表
CREATE TABLE `${TABLE_NAME_CONFIG}_history` (
    `history_id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
    `config_id` bigint NOT NULL COMMENT '关联的正式配置ID',
    `namespace` varchar(255) NOT NULL,
    `key` varchar(255) NOT NULL,
    `value` text NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `enabled` tinyint NOT NULL,
    `encrypted` tinyint NOT NULL,
    `version` varchar(50) NOT NULL COMMENT '版本号',
    `operation_type` varchar(20) NOT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`history_id`),
    KEY `idx_config_id` (`config_id`),
    KEY `idx_namespace_key` (`namespace`, `key`)
) DEFAULT CHARSET=utf8mb4 COMMENT='配置历史表';

-- 5. 创建 Route Draft 表
CREATE TABLE `${TABLE_NAME_ROUTE}_draft` (
    `id` varchar(100) NOT NULL COMMENT '路由ID',
    `uri` varchar(500) NOT NULL,
    `predicates` text,
    `filters` text,
    `metadata` text,
    `order_num` int DEFAULT 0,
    `enabled` tinyint(1) DEFAULT 1,
    `description` varchar(500),
    `operation_type` varchar(20) NOT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` varchar(100) DEFAULT NULL,
    `update_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='路由草稿表';

-- 6. 创建 Route History 表
CREATE TABLE `${TABLE_NAME_ROUTE}_history` (
    `history_id` bigint NOT NULL AUTO_INCREMENT,
    `route_id` varchar(100) NOT NULL,
    `uri` varchar(500) NOT NULL,
    `predicates` text,
    `filters` text,
    `metadata` text,
    `order_num` int DEFAULT 0,
    `enabled` tinyint(1) DEFAULT 1,
    `description` varchar(500),
    `version` varchar(50) NOT NULL,
    `operation_type` varchar(20) NOT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`history_id`),
    KEY `idx_route_id` (`route_id`)
) DEFAULT CHARSET=utf8mb4 COMMENT='路由历史表';
