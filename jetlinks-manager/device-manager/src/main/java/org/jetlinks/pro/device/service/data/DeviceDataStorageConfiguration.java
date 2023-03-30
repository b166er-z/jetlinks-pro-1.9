package org.jetlinks.pro.device.service.data;

import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.pro.device.service.data.influx.InfluxDBDeviceDataColumnStoragePolicy;
import org.jetlinks.pro.device.service.data.influx.InfluxDBDeviceDataRowStoragePolicy;
import org.jetlinks.pro.device.service.data.tdengine.TDengineDeviceDataColumnStoragePolicy;
import org.jetlinks.pro.influx.InfluxDBOperations;
import org.jetlinks.pro.tdengine.TDengineRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeviceDataStorageProperties.class)
public class DeviceDataStorageConfiguration {


    @Configuration
    @ConditionalOnClass(InfluxDBOperations.class)
    @ConditionalOnProperty(prefix = "influxdb", value = "enabled", havingValue = "true")
    static class InfluxDeviceDataStoreConfiguration {


        @Bean
        public InfluxDBDeviceDataRowStoragePolicy influxTimeRowSeriesDeviceDataStorePolicy(DeviceRegistry registry,
                                                                                           InfluxDBOperations operations,
                                                                                           DeviceDataStorageProperties properties) {
            return new InfluxDBDeviceDataRowStoragePolicy(
                registry,
                operations,
                properties
            );
        }

        @Bean
        public InfluxDBDeviceDataColumnStoragePolicy influxTimeSeriesDeviceDataColumnStorePolicy(DeviceRegistry registry,
                                                                                                 InfluxDBOperations operations,
                                                                                                 DeviceDataStorageProperties properties) {
            return new InfluxDBDeviceDataColumnStoragePolicy(
                registry,
                operations,
                properties
            );
        }


    }


    @Configuration
    @ConditionalOnClass(TDengineRepository.class)
    @ConditionalOnProperty(prefix = "tdengine", value = "enabled", havingValue = "true")
    static class TDengineeviceDataStoreConfiguration {

        @Bean
        public TDengineDeviceDataColumnStoragePolicy tDengineDeviceDataColumnStoragePolicy(DeviceRegistry registry,
                                                                                           TDengineRepository repository,
                                                                                           DeviceDataStorageProperties properties) {
            return new TDengineDeviceDataColumnStoragePolicy(
                registry,
                repository,
                properties
            );
        }
    }


}
