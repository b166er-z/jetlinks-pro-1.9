package org.jetlinks.pro.device.measurements.status;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.pro.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.pro.device.measurements.DeviceDashboardDefinition;
import org.jetlinks.pro.device.measurements.DeviceObjectDefinition;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.measurements.message.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.pro.micrometer.MeterRegistryManager;
import org.jetlinks.pro.timeseries.TimeSeriesManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Component
public class DeviceStatusMeasurementProvider extends StaticMeasurementProvider {

    private MeterRegistry registry;

    Map<String, LongAdder> productCounts = new ConcurrentHashMap<>();

    Function<String, LongAdder> counterAdder = productId ->
        productCounts.computeIfAbsent(productId, __id -> {
            LongAdder adder = new LongAdder();
            Gauge.builder("online-count", adder, LongAdder::sum)
                .tag("productId", __id)
                .register(registry);
            return adder;
        });

    public DeviceStatusMeasurementProvider(MeterRegistryManager registryManager,
                                           LocalDeviceInstanceService instanceService,
                                           TimeSeriesManager timeSeriesManager,
                                           EventBus eventBus) {
        super(DeviceDashboardDefinition.instance, DeviceObjectDefinition.status);

        addMeasurement(new DeviceStatusChangeMeasurement(timeSeriesManager, eventBus));

        addMeasurement(new DeviceStatusRecordMeasurement(instanceService, timeSeriesManager));

        registry = registryManager.getMeterRegister(DeviceTimeSeriesMetric.deviceMetrics().getId(),
            "target", "msgType", "productId");
    }

    @Subscribe("/device/*/*/online")
    public Mono<Void> incrementOnline(DeviceMessage msg){
        return Mono.fromRunnable(()->{
            String productId = parseProductId(msg);
            counterAdder.apply(productId).increment();
            registry
                .counter("online", "productId", productId)
                .increment();
        });
    }

    @Subscribe("/device/*/*/offline")
    public Mono<Void> incrementOffline(DeviceMessage msg){
        return Mono.fromRunnable(()->{
            String productId = parseProductId(msg);
           // counterAdder.apply(productId).decrement();
            registry
                .counter("offline", "productId", productId)
                .increment();
        });
    }

    private String parseProductId(DeviceMessage msg) {
        return msg
            .getHeader("productId")
            .map(String::valueOf)
            .orElse("unknown");
    }
}
