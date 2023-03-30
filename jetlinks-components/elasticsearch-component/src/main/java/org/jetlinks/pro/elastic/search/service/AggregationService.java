package org.jetlinks.pro.elastic.search.service;

import org.jetlinks.pro.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface AggregationService {

    Flux<Map<String, Object>> aggregation(String[] index, AggregationQueryParam queryParam);

}
