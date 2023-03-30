package org.jetlinks.pro.influx;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "influxdb", value = "enabled", havingValue = "true")
@EnableConfigurationProperties(InfluxDBProperties.class)
public class InfluxDBConfiguration {

    @Bean(destroyMethod = "shutdown",initMethod = "init")
    public InfluxDBTemplate influxDBOperations(InfluxDBProperties properties) {
        InfluxDBTemplate template= new InfluxDBTemplate(properties.getDatabase(), properties.createClient());
        template.setBufferRate(properties.getBufferRate());
        template.setBufferSize(properties.getBufferSize());
        template.setBufferTimeout(properties.getBufferTimeout());
        template.setMaxRetry(properties.getMaxRetry());
        template.setBackpressureBufferSize(properties.getMaxBufferSize());
        template.setMaxBufferBytes(properties.getMaxBufferBytes().toBytes());
        return template;
    }

}
