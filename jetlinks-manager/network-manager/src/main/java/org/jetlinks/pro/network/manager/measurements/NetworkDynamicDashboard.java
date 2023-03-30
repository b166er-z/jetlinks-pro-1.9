package org.jetlinks.pro.network.manager.measurements;

import org.jetlinks.pro.dashboard.DashboardObject;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class NetworkDynamicDashboard implements NetworkDashboard {
    @Override
    public Flux<DashboardObject> getObjects() {
        return Flux.just(new NetworkDashboardObject());
    }

    @Override
    public Mono<DashboardObject> getObject(String id) {
        return Mono.just(new NetworkDashboardObject());
    }
}
