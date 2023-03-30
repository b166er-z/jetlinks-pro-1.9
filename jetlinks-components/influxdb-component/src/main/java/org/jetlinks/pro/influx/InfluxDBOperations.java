package org.jetlinks.pro.influx;

import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.jetlinks.pro.timeseries.TimeSeriesData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * influxdb操作接口
 *
 * @author zhouhao
 * @see InfluxDBTemplate
 * @since 1.5.0
 */
public interface InfluxDBOperations {

    /**
     * 写入单条数据,该操作会根据情况进行buffer后再批量写入
     *
     * @param point 数据
     * @return void
     */
    Mono<Void> write(Point point);

    /**
     * 写入批量数据,该操作会立即写入数据
     *
     * @param points 数据
     * @return void
     */
    Mono<Void> write(BatchPoints points);

    /**
     * SQL查询数据
     *
     * @param sql SQL
     * @return 数据集
     */
    Flux<TimeSeriesData> query(String sql);

}
