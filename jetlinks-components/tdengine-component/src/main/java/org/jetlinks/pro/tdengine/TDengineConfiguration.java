package org.jetlinks.pro.tdengine;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;

@Configuration
@ConditionalOnProperty(prefix = "tdengine", value = "enabled", havingValue = "true")
@EnableConfigurationProperties(TDengineProperties.class)
public class TDengineConfiguration {

    @Bean
    @ConditionalOnMissingBean(TDengineOperations.class)
    public TDengineOperations tDengineOperations(TDengineProperties properties) {

        if (properties.getConnector() == TDengineProperties.Connector.restful) {
            return new RestfulTDengineTemplate(properties.getRestful().createClient());
        }

        if (properties.getConnector() == TDengineProperties.Connector.jdbc) {
            return new JdbcTDengineOperations(
                properties.getJdbc().createDataSource(),
                Schedulers.boundedElastic()
            );
        }

        throw new UnsupportedOperationException("unsupported tdengine connector:" + properties.getConnector());
    }

    @Bean(destroyMethod = "shutdown")
    public DefaultTDengineRepository tDengineRepository(TDengineProperties properties, TDengineOperations operations) {
        return new DefaultTDengineRepository(properties, operations);
    }

}
