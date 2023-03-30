package org.jetlinks.pro.timeseries.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.pro.timeseries.TimeSeriesManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeSeriesMeterRegistryConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "micrometer.time-series")
    public TimeSeriesMeterRegistrySupplier timeSeriesMeterRegistrySupplier(TimeSeriesManager timeSeriesManager){
        return new TimeSeriesMeterRegistrySupplier(timeSeriesManager);
    }

    @Bean
    public MeterRegistry meterRegistry(TimeSeriesMeterRegistrySupplier registrySupplier){
        return registrySupplier.getMeterRegistry("jetlinks-metrics");
    }

}
