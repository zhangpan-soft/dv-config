package com.dv.config.server.impl.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.dv.config.server.gateway.ConfigGateway;
import com.dv.config.server.gateway.RouteGateway;
import com.dv.config.server.impl.gateway.ConfigGatewayImpl;
import com.dv.config.server.impl.gateway.RouteGatewayImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

@Configuration
@ComponentScan(
    basePackages = "com.dv.config.server.impl",
    excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class)
)
@MapperScan(value = {"com.dv.config.server.impl.mapper"})
public class ApiImplConfigure {

    @Bean
    public RouteGateway routeGateway(){
        return new RouteGatewayImpl();
    }
    @Bean
    public ConfigGateway configGateway(){
        return new ConfigGatewayImpl();
    }

    @Value("${spring.liquibase.parameters.TABLE_NAME_CONFIG}")
    private String configTableName;
    @Value("${spring.liquibase.parameters.TABLE_NAME_ROUTE}")
    private String routeTableName;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 动态表名插件
        DynamicTableNameInnerInterceptor dynamicTableNameInterceptor = new DynamicTableNameInnerInterceptor((sql, tableName) -> {
            if ("config".equalsIgnoreCase(tableName)){
                return configTableName;
            }
            if ("route".equalsIgnoreCase(tableName)){
                return routeTableName;
            }
            // 处理 Draft 和 History 表的动态表名
            if ("config_draft".equalsIgnoreCase(tableName)) {
                return configTableName + "_draft";
            }
            if ("config_history".equalsIgnoreCase(tableName)) {
                return configTableName + "_history";
            }
            if ("route_draft".equalsIgnoreCase(tableName)) {
                return routeTableName + "_draft";
            }
            if ("route_history".equalsIgnoreCase(tableName)) {
                return routeTableName + "_history";
            }
            return tableName;
        });
        interceptor.addInnerInterceptor(dynamicTableNameInterceptor);
        return interceptor;
    }
}
