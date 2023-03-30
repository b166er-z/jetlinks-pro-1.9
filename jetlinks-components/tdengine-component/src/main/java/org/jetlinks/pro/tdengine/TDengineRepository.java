package org.jetlinks.pro.tdengine;

import org.jetlinks.pro.timeseries.TimeSeriesData;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Set;

public interface TDengineRepository {

    String getTimestampColumn();

    String getDatabase();

    String getTableForQuery(String tableName);

    String convertColumnName(String column);

    Mono<Void> registerMetadata(TimeSeriesMetadata metadata,
                                Set<String> tags,
                                boolean ddl);

    Mono<Void> insert(Point data);

    Mono<Void> insert(Collection<Point> batch);

    Flux<TimeSeriesData> query(String sql);

}
