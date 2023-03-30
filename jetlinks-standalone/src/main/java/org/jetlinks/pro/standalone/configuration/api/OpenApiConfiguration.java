package org.jetlinks.pro.standalone.configuration.api;

import org.jetlinks.pro.openapi.OpenApiClientManager;
import org.jetlinks.pro.openapi.interceptor.OpenApiFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenApiFilter openApiFilter(OpenApiClientManager clientManager) {
        return new OpenApiFilter(clientManager);
    }

}
