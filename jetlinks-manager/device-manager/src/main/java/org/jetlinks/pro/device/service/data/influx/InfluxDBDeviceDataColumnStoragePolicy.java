package org.jetlinks.pro.device.service.data.influx;

import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.service.data.DeviceDataStorageProperties;
import org.jetlinks.pro.device.measurements.message.timeseries.DeviceTimeSeriesMetadata;
import org.jetlinks.pro.influx.InfluxDBOperations;
import org.jetlinks.pro.timeseries.TimeSeriesData;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import org.jetlinks.pro.timeseries.query.AggregationData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InfluxDBDeviceDataColumnStoragePolicy extends InfluxDBDeviceDataStoragePolicy {

    public InfluxDBDeviceDataColumnStoragePolicy(DeviceRegistry registry,
                                                 InfluxDBOperations operations,
                                                 DeviceDataStorageProperties properties) {
        super(registry, operations, properties);
    }

    @Override
    public String getId() {
        return "influx-column";
    }

    @Override
    public String getName() {
        return "InfluxDB-列式存储";
    }

    @Override
    public String getDescription() {
        return "每个设备的全部属性为一行数据.需要设备每次上报全部属性.";
    }

    @Override
    protected List<TimeSeriesMetadata> createTimeSeriesMetadata(String productId, DeviceMetadata metadata) {
        List<TimeSeriesMetadata> tsMetadata = new ArrayList<>(16);

        for (EventMetadata event : metadata.getEvents()) {
            tsMetadata.add(DeviceTimeSeriesMetadata.event(productId, event));
        }
        tsMetadata.add(DeviceTimeSeriesMetadata.properties(productId, metadata.getProperties()));
        tsMetadata.add(DeviceTimeSeriesMetadata.log(productId));
        return tsMetadata;
    }

    @Override
    protected Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId,
                                                                     DeviceMessage message,
                                                                     Map<String, Object> properties) {

        return convertPropertiesForColumnPolicy(productId, message, properties);
    }

    @Override
    protected Flux<AggregationData> aggregationPropertiesByDevice(Mono<Tuple2<DeviceProductOperator, DeviceMetadata>> productAndMetadata,
                                                                  DeviceDataService.AggregationRequest request,
                                                                  DeviceDataService.DevicePropertyAggregation... properties) {
        return productAndMetadata.flatMapMany(tp2 -> {
            StringJoiner joiner = new StringJoiner("", "select ", "");
            int idx = 0;
            for (DeviceDataService.DevicePropertyAggregation property : properties) {
                if (idx++ > 0) {
                    joiner.add(",");
                }
                joiner.add(getAggFunction(property.getAgg()))
                      .add("(\"").add(property.getProperty()).add("\")")
                      .add(" as \"").add(property.getAlias()).add("\"");
            }
            String metric = getPropertyTimeSeriesMetric(tp2.getT1().getId());

            joiner
                .add(" from \"").add(metric).add("\" ")
                .add(buildWhere(
                    metric,
                    request.getFilter().and("time", "btw", Arrays.asList(request.getFrom(), request.getTo())))
                );

            if (request.getInterval() != null) {
                joiner.add(" group by ")
                      .add(getGroupByTime(request.getInterval()));
            }
            joiner.add(buildOrderBy(request.getFilter()));

            String format = request.getFormat();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

            return operations
                .query(joiner.toString())
                .map(timeSeriesData -> {
                    long ts = timeSeriesData.getTimestamp();
                    Map<String, Object> newData = timeSeriesData.getData();
                    for (DeviceDataService.DevicePropertyAggregation property : properties) {
                        newData.putIfAbsent(property.getAlias(), 0);
                    }
                    newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())));
                    return AggregationData.of(newData);
                })
                .take(request.getLimit());

        });

    }


    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                                    @Nonnull QueryParamEntity query) {


        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMapMany(productAndMetadata -> this
                .doQuery(getPropertyTimeSeriesMetric(productAndMetadata.getT1().getId())
                    , query.and("deviceId", "eq", deviceId)
                    , Function.identity())
                .flatMap(data -> rowToProperty(data, productAndMetadata.getT2().getProperties())));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryProperty(@Nonnull String deviceId,
                                              @Nonnull QueryParamEntity query,
                                              @Nonnull String... property) {
        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMapMany(productAndMetadata -> queryProperty(productAndMetadata, query.and("deviceId", "eq", deviceId), property));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryPropertyByProductId(@Nonnull String productId,
                                                         @Nonnull QueryParamEntity query,
                                                         @Nonnull String... property) {
        return this
            .getProductAndMetadataByProduct(productId)
            .flatMapMany(productAndMetadata -> queryProperty(productAndMetadata, query, property));
    }

    private Flux<DeviceProperty> queryProperty(@Nonnull Tuple2<DeviceProductOperator, DeviceMetadata> productAndMetadata,
                                               @Nonnull QueryParamEntity query,
                                               @Nonnull String... property) {
        Set<String> includes = new HashSet<>(Arrays.asList(property));
        Map<String, PropertyMetadata> propertiesMap = productAndMetadata
            .getT2()
            .getProperties()
            .stream()
            .filter(prop -> includes.size() == 0 || includes
                .contains(prop.getId()))
            .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));

        return this
            .doQuery(getPropertyTimeSeriesMetric(productAndMetadata.getT1().getId())
                , query
                , Function.identity())
            .flatMap(data -> rowToProperty(data, propertiesMap.values()));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                               @Nonnull String property,
                                                               @Nonnull QueryParamEntity query) {

        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMap(productAndMetadata -> queryPropertyPage(productAndMetadata, property, query.and("deviceId", TermType.eq, deviceId)));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPageByProductId(@Nonnull String productId,
                                                                          @Nonnull String property,
                                                                          @Nonnull QueryParamEntity query) {
        return this
            .getProductAndMetadataByProduct(productId)
            .flatMap(productAndMetadata -> queryPropertyPage(productAndMetadata, property, query));
    }

    private Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull Tuple2<DeviceProductOperator, DeviceMetadata> productAndMetadata,
                                                                @Nonnull String property,
                                                                @Nonnull QueryParamEntity query) {
        PropertyMetadata prop = productAndMetadata.getT2().getPropertyOrNull(property);

        return this
            .doQueryPager(getPropertyTimeSeriesMetric(productAndMetadata.getT1().getId())
                , query
                , data -> DeviceProperty
                    .of(data, data.get(property).orElse(null), prop)
                    .property(property), data -> data.get(property).isPresent()
            );
    }
}
