package org.jetlinks.pro.elastic.search.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cache.Caches;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.pro.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.pro.elastic.search.service.AggregationService;
import org.jetlinks.pro.elastic.search.service.ElasticSearchService;
import org.jetlinks.pro.timeseries.TimeSeriesManager;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import org.jetlinks.pro.timeseries.TimeSeriesMetric;
import org.jetlinks.pro.timeseries.TimeSeriesService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Service
@Slf4j
public class ElasticSearchTimeSeriesManager implements TimeSeriesManager {


    private final Map<String, TimeSeriesService> serviceMap = Caches.newCache();

    protected final ElasticSearchIndexManager indexManager;

    private final ElasticSearchService elasticSearchService;

    private final AggregationService aggregationService;


    public ElasticSearchTimeSeriesManager(ElasticSearchIndexManager indexManager,
                                          ElasticSearchService elasticSearchService,
                                          AggregationService aggregationService) {
        this.elasticSearchService = elasticSearchService;
        this.indexManager = indexManager;
        this.aggregationService = aggregationService;
    }

    @Override
    public TimeSeriesService getService(TimeSeriesMetric metric) {
        return getService(metric.getId());
    }

    @Override
    public TimeSeriesService getServices(TimeSeriesMetric... metric) {
        return getServices(Arrays
            .stream(metric)
            .map(TimeSeriesMetric::getId).toArray(String[]::new));
    }

    @Override
    public TimeSeriesService getServices(String... metric) {
        return new ElasticSearchTimeSeriesService(metric, elasticSearchService, aggregationService);
    }

    @Override
    public TimeSeriesService getService(String metric) {
        return serviceMap.computeIfAbsent(metric,
            id -> new ElasticSearchTimeSeriesService(new String[]{id}, elasticSearchService, aggregationService));
    }


    @Override
    public Mono<Void> registerMetadata(TimeSeriesMetadata metadata) {
        //默认字段
        SimplePropertyMetadata timestamp = new SimplePropertyMetadata();
        timestamp.setId("timestamp");
        timestamp.setValueType(new DateTimeType());
        return indexManager.putIndex(new DefaultElasticSearchIndexMetadata(metadata.getMetric().getId(), metadata.getProperties())
            .addProperty(timestamp));
    }

}
