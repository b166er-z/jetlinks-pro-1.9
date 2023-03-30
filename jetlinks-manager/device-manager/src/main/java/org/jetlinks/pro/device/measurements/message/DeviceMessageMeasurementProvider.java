package org.jetlinks.pro.device.measurements.message;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.pro.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.pro.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.pro.device.measurements.DeviceObjectDefinition;
import org.jetlinks.pro.device.measurements.message.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.micrometer.MeterRegistryManager;
import org.jetlinks.pro.timeseries.TimeSeriesManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DeviceMessageMeasurementProvider extends StaticMeasurementProvider {

    MeterRegistry registry;

    public DeviceMessageMeasurementProvider(EventBus eventBus,
                                            MeterRegistryManager registryManager,
                                            DeviceRegistry deviceRegistry,
                                            TimeSeriesManager timeSeriesManager) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.message);

        registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId(),
            "target", "msgType", "productId");

        addMeasurement(new DeviceMessageMeasurement(eventBus, deviceRegistry, timeSeriesManager));

    }

    @Subscribe("/device/*/*/message/**")
    public Mono<Void> incrementMessage(DeviceMessage message) {
        registry
            .counter("message-count",  "productId",message.getHeader("productId").map(String::valueOf).orElse("unknown"))
            .increment();
        return Mono.empty();
    }

}
