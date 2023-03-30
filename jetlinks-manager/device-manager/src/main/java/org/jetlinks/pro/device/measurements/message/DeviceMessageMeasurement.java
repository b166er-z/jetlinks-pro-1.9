package org.jetlinks.pro.device.measurements.message;

import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.dashboard.*;
import org.jetlinks.pro.dashboard.supports.StaticMeasurement;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.device.measurements.message.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.pro.tenant.TenantAsset;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.timeseries.TimeSeriesManager;
import org.jetlinks.pro.timeseries.TimeSeriesMetric;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.jetlinks.pro.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class DeviceMessageMeasurement extends StaticMeasurement {

    private final EventBus eventBus;

    private final TimeSeriesManager timeSeriesManager;

    private final DeviceRegistry deviceRegistry;
    static MeasurementDefinition definition = MeasurementDefinition.of("quantity", "设备消息量");

    public DeviceMessageMeasurement(EventBus eventBus,
                                    DeviceRegistry registry,
                                    TimeSeriesManager timeSeriesManager) {
        super(definition);
        this.deviceRegistry = registry;
        this.eventBus = eventBus;
        this.timeSeriesManager = timeSeriesManager;
        addDimension(new RealTimeMessageDimension());
        addDimension(new AggMessageDimension());
    }

    static ConfigMetadata realTimeConfigMetadata = new DefaultConfigMetadata()
        .add("interval", "数据统计周期", "例如: 1s,10s", new StringType());

    class RealTimeMessageDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return IntType.GLOBAL;
        }

        @Override
        public ConfigMetadata getParams() {
            return realTimeConfigMetadata;
        }

        @Override
        public boolean isRealTime() {
            return true;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {


            //通过订阅消息来统计实时数据量
            return eventBus
                .subscribe(Subscription.of("real-time-device-message", "/device/**", Subscription.Feature.local, Subscription.Feature.broker))
                .doOnNext(TopicPayload::release)
                .window(parameter.getDuration("interval").orElse(Duration.ofSeconds(1)))
                .flatMap(Flux::count)
                .map(total -> SimpleMeasurementValue.of(total, System.currentTimeMillis()));
        }
    }


    static ConfigMetadata historyConfigMetadata = new DefaultConfigMetadata()
        .add("productId", "设备型号", "", new StringType())
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("msgType", "消息类型", "", new StringType())
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType())
        .add("to", "时间至", "", new DateTimeType());

    class AggMessageDimension implements MeasurementDimension {


        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
        }

        @Override
        public DataType getValueType() {
            return IntType.GLOBAL;
        }

        @Override
        public ConfigMetadata getParams() {
            return historyConfigMetadata;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        private AggregationQueryParam createQueryParam(MeasurementParameter parameter) {
            return AggregationQueryParam.of()
//                .sum("count")
                .groupBy(
                    parameter.getInterval("time").orElse(Interval.ofHours(1)),
                    parameter.getString("format").orElse("MM月dd日 HH时"))
//                .filter(query ->
//                    query
//                        .where("name", "message-count")
//                        .is("productId", parameter.getString("productId").orElse(null))
//                        .is("msgType", parameter.getString("msgType").orElse(null))
//                )
                .limit(parameter.getInt("limit").orElse(1))
                .from(parameter.getDate("from").orElseGet(() -> Date.from(LocalDateTime.now().plusDays(-1).atZone(ZoneId.systemDefault()).toInstant())))
                .to(parameter.getDate("to").orElse(new Date()));
        }

        private Mono<TimeSeriesMetric[]> getProductMetrics(List<String> productIdList) {
            return Flux
                .fromIterable(productIdList)
                .flatMap(id -> deviceRegistry
                    .getProduct(id)
                    .flatMap(DeviceProductOperator::getMetadata)
                    .onErrorResume(err -> Mono.empty())
                    .flatMapMany(metadata -> Flux.fromIterable(metadata.getEvents())
                        .map(event -> DeviceTimeSeriesMetric.deviceEventMetric(id, event.getId())))
                    .concatWithValues(DeviceTimeSeriesMetric.devicePropertyMetric(id)))
                .collectList()
                .map(list -> list.toArray(new TimeSeriesMetric[0]));
        }

        @Override
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {
            String productId = parameter.getString("productId").orElse(null);

            return TenantMember.current()
                .map(member -> {
                    Flux<TenantAsset> productIdStream = productId == null
                        ? member.getAssets(DeviceAssetType.product)
                        : member.getAsset(DeviceAssetType.product, productId).flux();
                    return productIdStream
                        .map(TenantAsset::getAssetId)
                        .collectList()
                        .flatMap(this::getProductMetrics)
                        .flatMapMany(metrics ->
                            member
                                .getAssets("device")
                                .map(TenantAsset::getAssetId)
                                .buffer(1000)
                                .flatMap(idList -> this
                                    .createQueryParam(parameter)
                                    .count("deviceId", "count")
                                    .filter(query -> query.in("deviceId", idList))
                                    .execute(timeSeriesManager.getServices(metrics)::aggregation)
                                ))
                        .as(flux -> flux
                            //按时间分组,再聚合起来
                            .groupBy(data -> data.getString("time", ""),Integer.MAX_VALUE)
                            .flatMap(group -> group
                                .reduce((aggregationData, aggregationData2) -> {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("count", aggregationData.getInt("count", 0) + aggregationData2.getInt("count", 0));
                                    data.put("time", group.key());
                                    return AggregationData.of(data);
                                })))
                        .index((index, data) ->
                            SimpleMeasurementValue.of(
                                data.getInt("count").orElse(0),
                                data.getString("time").orElse(""),
                                data.getDate("time").map(Date::getTime).orElse(index))
                        )
                        .sort(MeasurementValue.asc);
                })
                .defaultIfEmpty(Flux.defer(() ->
                    AggregationQueryParam.of()
                        .sum("count")
                        .groupBy(
                            parameter.getInterval("time").orElse(Interval.ofHours(1)),
                            parameter.getString("format").orElse("MM月dd日 HH时"))
                        .filter(query ->
                            query.where("name", "message-count")
                                .is("productId", parameter.getString("productId").orElse(null))
                                .is("msgType", parameter.getString("msgType").orElse(null))
                        )
                        .limit(parameter.getInt("limit").orElse(1))
                        .from(parameter.getDate("from").orElseGet(() -> Date.from(LocalDateTime.now().plusDays(-1).atZone(ZoneId.systemDefault()).toInstant())))
                        .to(parameter.getDate("to").orElse(new Date()))
                        .execute(timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceMetrics())::aggregation)
                        .index((index, data) -> SimpleMeasurementValue.of(
                            data.getInt("count").orElse(0),
                            data.getString("time").orElse(""),
                            index))
                        .sort()))
                .flatMapMany(Function.identity());
        }
    }

}
