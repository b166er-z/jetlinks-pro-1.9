package org.jetlinks.pro.network.manager.measurements;

import org.jetlinks.pro.dashboard.DashboardObject;
import org.jetlinks.pro.dashboard.Measurement;
import org.jetlinks.pro.dashboard.MeasurementDefinition;
import org.jetlinks.pro.dashboard.ObjectDefinition;
import org.jetlinks.pro.dashboard.supports.StaticMeasurement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class NetworkDashboardObject implements DashboardObject {



    @Override
    public ObjectDefinition getDefinition() {
        return NetworkObjectDefinition.traffic;
    }

    static StaticMeasurement measurement=new StaticMeasurement(MeasurementDefinition.of("traffic","流量"));

    @Override
    public Flux<Measurement> getMeasurements() {
        return Flux.just(measurement);
    }

    @Override
    public Mono<Measurement> getMeasurement(String id) {
        return Mono.just(measurement);
    }
}
