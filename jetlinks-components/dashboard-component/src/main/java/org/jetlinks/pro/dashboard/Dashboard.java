package org.jetlinks.pro.dashboard;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface Dashboard {

    DashboardDefinition getDefinition();

    Flux<DashboardObject> getObjects();

    Mono<DashboardObject> getObject(String id);

}
