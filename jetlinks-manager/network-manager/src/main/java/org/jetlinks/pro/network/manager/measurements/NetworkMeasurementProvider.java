package org.jetlinks.pro.network.manager.measurements;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.dashboard.*;
import org.jetlinks.pro.dashboard.supports.StaticMeasurement;
import org.jetlinks.pro.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.pro.timeseries.TimeSeriesManager;
import org.jetlinks.pro.timeseries.TimeSeriesMetric;
import org.jetlinks.pro.timeseries.query.AggregationQueryParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class NetworkMeasurementProvider extends StaticMeasurementProvider {

    private TimeSeriesManager timeSeriesManager;

    public NetworkMeasurementProvider(TimeSeriesManager timeSeriesManager) {
        super(NetworkDashboardDefinition.instance, NetworkObjectDefinition.traffic);
        this.timeSeriesManager = timeSeriesManager;
        addMeasurement(NetworkDashboardObject.measurement.addDimension(new NetworkMeasurementDimension()));
    }

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("target", "发送/接收数据", "例如：bytesSent/bytesRead", new StringType())
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType())
        .add("to", "时间至", "", new DateTimeType());

    class NetworkMeasurementDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return DimensionDefinition.of("network", "网络流量");
        }

        @Override
        public DataType getValueType() {
            return new DoubleType().scale(2);
        }

        @Override
        public ConfigMetadata getParams() {
            return configMetadata;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        @Override
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {
            return AggregationQueryParam.of()
                .sum("count")
                .groupBy(parameter.getInterval("time").orElse(Interval.ofDays(1)),
                    parameter.getString("format").orElse("MM月dd日"))
                .filter(query -> query.is("target", parameter.getString("target").orElse("bytesSent")))
                .limit(parameter.getInt("limit").orElse(30))
                .from(parameter.getDate("from").orElseGet(() ->
                    Date.from(LocalDateTime.now().plusDays(-30).atZone(ZoneId.systemDefault()).toInstant())))
                .to(parameter.getDate("to").orElse(new Date()))
                .execute(timeSeriesManager.getService(TimeSeriesMetric.of("net_monitor"))::aggregation)
                .index((index, data) -> SimpleMeasurementValue.of(
                    data.getInt("count").orElse(0) / 1024,
                    data.getString("time").orElse(""),
                    index))
                .sort();
        }
    }
}
