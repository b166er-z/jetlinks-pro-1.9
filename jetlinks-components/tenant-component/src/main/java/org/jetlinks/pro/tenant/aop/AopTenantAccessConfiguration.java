package org.jetlinks.pro.tenant.aop;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class AopTenantAccessConfiguration {


    @Bean
    @ConditionalOnProperty(prefix = "jetlinks.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AopTenantAccessSupport aopTenantAccessHandler(@Lazy TransactionalOperator transactionalOperator) {
        AopTenantAccessSupport support = new AopTenantAccessSupport();
        support.setAdvice(new AopTenantAccessHandler(transactionalOperator));
        return support;
    }

}
