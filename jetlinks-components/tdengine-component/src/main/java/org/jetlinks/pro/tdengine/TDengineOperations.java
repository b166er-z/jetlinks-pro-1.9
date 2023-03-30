package org.jetlinks.pro.tdengine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface TDengineOperations {

    Mono<Void> execute(String sql);

    Flux<Map<String,Object>> query(String sql);

}
