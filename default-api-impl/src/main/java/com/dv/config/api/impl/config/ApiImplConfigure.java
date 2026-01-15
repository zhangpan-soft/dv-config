package com.dv.config.api.impl.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.dv.config.api.gateway.ConfigGateway;
import com.dv.config.api.gateway.RouteGateway;
import com.dv.config.api.impl.gateway.ConfigGatewayImpl;
import com.dv.config.api.impl.gateway.RouteGatewayImpl;
import com.dv.config.common.crypto.CryptoProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

@Configuration
@ComponentScan(
    basePackages = "com.dv.config.api.impl",
    excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class)
)
@MapperScan(value = {"com.dv.config.api.impl.mapper"})
public class ApiImplConfigure {

    @Bean
    @ConditionalOnMissingBean(CryptoProperties.class)
    @ConfigurationProperties(prefix = "netty.crypto")
    public CryptoProperties cryptoProperties(){
        return new CryptoProperties();
    }

    @Bean
    public RouteGateway routeGateway(){
        return new RouteGatewayImpl();
    }
    @Bean
    public ConfigGateway configGateway(){
        return new ConfigGatewayImpl();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(@Value("${TABLE_NAME_CONFIG:config}") String configTableName,
                                                         @Value("${TABLE_NAME_ROUTE:route}") String routeTableName) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
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
