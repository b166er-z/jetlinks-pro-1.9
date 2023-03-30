package org.jetlinks.pro.device.service.data.influx;

import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.service.data.DeviceDataStorageProperties;
import org.jetlinks.pro.device.measurements.message.timeseries.DeviceTimeSeriesMetadata;
import org.jetlinks.pro.influx.InfluxDBOperations;
import org.jetlinks.pro.timeseries.TimeSeriesData;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 使用influxDB存储设备消息数据，以产品分表，每一个属性值为一行数据。
 *
 * @since  1.5.0
 */
public class InfluxDBDeviceDataRowStoragePolicy extends InfluxDBDeviceDataStoragePolicy {

    public InfluxDBDeviceDataRowStoragePolicy(DeviceRegistry registry,
                                              InfluxDBOperations operations,
                                              DeviceDataStorageProperties properties) {
        super(registry, operations, properties);
    }

    @Override
    public String getId() {
        return "influx-row";
    }

    @Override
    public String getName() {
        return "InfluxDB-行式存储";
    }

    @Override
    public String getDescription() {
        return "每个设备的每一个属性为一行数据.适合设备每次上报部分属性.";
    }


    @Override
    protected List<TimeSeriesMetadata> createTimeSeriesMetadata(String productId, DeviceMetadata metadata) {
        List<TimeSeriesMetadata> tsMetadata = new ArrayList<>(16);
        for (EventMetadata event : metadata.getEvents()) {
            tsMetadata.add(DeviceTimeSeriesMetadata.event(productId, event));
        }
        tsMetadata.add(DeviceTimeSeriesMetadata.properties(productId));
        tsMetadata.add(DeviceTimeSeriesMetadata.log(productId));
        return tsMetadata;
    }

    @Override
    protected Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId,
                                                                     DeviceMessage message,
                                                                     Map<String, Object> properties) {

        return convertPropertiesForRowPolicy(productId, message, properties);
    }


    @Nonnull
    @Override
    public Flux<DeviceProperty> queryProperty(@Nonnull String deviceId,
                                              @Nonnull QueryParamEntity query,
                                              @Nonnull String... property) {

        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMapMany(tp2 -> queryProperty(tp2, query.and("deviceId", "eq", deviceId), property));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryPropertyByProductId(@Nonnull String productId,
                                                         @Nonnull QueryParamEntity query,
                                                         @Nonnull String... property) {
        return this
            .getProductAndMetadataByProduct(productId)
            .flatMapMany(tp2 -> queryProperty(tp2, query, property));
    }

    private Flux<DeviceProperty> queryProperty(@Nonnull Tuple2<DeviceProductOperator, DeviceMetadata> productAndMetadata,
                                               @Nonnull QueryParamEntity query,
                                               @Nonnull String... property) {

        return query
            .toQuery()
            .when(property.length > 0, that -> that.in("property", Arrays.asList(property)))
            .execute(param -> this
                .doQuery(
                    getPropertyTimeSeriesMetric(productAndMetadata.getT1().getId()),
                    param,
                    timeSeriesData -> DeviceProperty
                        .of(timeSeriesData,
                            timeSeriesData
                                .getString("property")
                                .flatMap(productAndMetadata.getT2()::getProperty)
                                .orElse(null))));
    }

    @Override
    protected void fillRowPropertyValue(Map<String, Object> target, PropertyMetadata property, Object value) {
        if (value == null) {
            return;
        }
        if (property == null) {
            if (value instanceof Number) {
                target.put("numberValue", value);
            } else if (value instanceof Date) {
                target.put("timeValue", value);
            }
            target.put("value", String.valueOf(value));
            return;
        }
        DataType type = property.getValueType();
        target.put("type", type.getId());
        if (type instanceof NumberType) {
            NumberType<?> numberType = (NumberType<?>) type;
            Number number = numberType.convertNumber(value);
            if (number == null) {
                throw new UnsupportedOperationException("无法将" + value + "转为" + type.getId());
            }
            target.put("numberValue", number);
        } else if (type instanceof DateTimeType) {
            DateTimeType dateTimeType = (DateTimeType) type;
            target.put("timeValue", dateTimeType.convert(value));
        }
        target.put("value", String.valueOf(value));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                               @Nonnull String property,
                                                               @Nonnull QueryParamEntity query) {
        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMap(tp2 -> queryPropertyPage(tp2, property, query.and("deviceId", TermType.eq, deviceId)));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPageByProductId(@Nonnull String productId,
                                                                          @Nonnull String property,
                                                                          @Nonnull QueryParamEntity query) {
        return this
            .getProductAndMetadataByProduct(productId)
            .flatMap(tp2 -> queryPropertyPage(tp2, property, query));
    }

    private Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull Tuple2<DeviceProductOperator, DeviceMetadata> productAndMetadata,
                                                                @Nonnull String property,
                                                                @Nonnull QueryParamEntity query) {
        String productId = productAndMetadata.getT1().getId();

        return query
            .toQuery()
            .and("property", property)
            .execute(param -> this
                .doQueryPager(getPropertyTimeSeriesMetric(productId),
                              param,
                              timeSeriesData -> DeviceProperty.of(
                                  timeSeriesData,
                                  timeSeriesData
                                      .getString("property")
                                      .flatMap(productAndMetadata.getT2()::getProperty)
                                      .orElse(null))));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                                    @Nonnull QueryParamEntity query) {
        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMapMany(tp2 -> {
                String productId = tp2.getT1().getId();
                query.and("deviceId", "eq", deviceId);
                String metric = getPropertyTimeSeriesMetric(productId);

                //按属性分组，取指定数量
                String sql = String.join("",
                                         "select * from \"", metric, "\" ",
                                         buildWhere(metric, query),
                                         " group by property " + buildOrderBy(query) + " limit ", String.valueOf(query.getPageSize())
                );
                return operations
                    .query(sql)
                    .map(timeSeriesData -> DeviceProperty
                        .of(timeSeriesData,
                            timeSeriesData.getString("property").flatMap(tp2.getT2()::getProperty).orElse(null)));
            });
    }

    @Override
    protected Flux<AggregationData> aggregationPropertiesByDevice(Mono<Tuple2<DeviceProductOperator, DeviceMetadata>> productAndMetadata,
                                                                  DeviceDataService.AggregationRequest request,
                                                                  DeviceDataService.DevicePropertyAggregation... properties) {
        return productAndMetadata
            .flatMapMany(tp2 -> {
                String productId = tp2.getT1().getId();

                //过滤属性
                StringJoiner propertyIn = new StringJoiner(" or ", "(", ")");
                for (DeviceDataService.DevicePropertyAggregation s : properties) {
                    propertyIn.add("property='" + s.getProperty() + "'");
                }
                //聚合
                StringJoiner agg = new StringJoiner("");
                int index = 0;
                for (DeviceDataService.DevicePropertyAggregation property : properties) {
                    if (index++ > 0) {
                        agg.add(",");
                    }
                    agg.add(getAggFunction(property.getAgg()))
                       .add("(numberValue)").add(" as ").add("value_" + property.getAlias());
                }
                String metric = getPropertyTimeSeriesMetric(productId);

                String sql = String.join(
                    "",
                    "\"", metric, "\" ",
                    buildWhere(metric,
                               request
                                   .getFilter()
                                   .and("time", "btw", Arrays.asList(request.getFrom(), request.getTo())),
                               propertyIn.toString()
                    )
                );
                String dataSql = "select " + agg + " from " + sql + " group by property";
                if (request.getInterval() != null) {
                    dataSql += ",";
                    dataSql += getGroupByTime(request.getInterval());
                }
                String format = request.getFormat();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

                if (properties.length == 1) {
                    String key = "value_" + properties[0].getAlias();
                    return operations
                        .query(dataSql)
                        .sort(Comparator.comparing(TimeSeriesData::getTimestamp).reversed())
                        .map(timeSeriesData -> {
                            long ts = timeSeriesData.getTimestamp();
                            Map<String, Object> newData = new HashMap<>();
                            newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId
                                .systemDefault())));
                            newData.put(properties[0].getAlias(), timeSeriesData.get(key).orElse(0));

                            return AggregationData.of(newData);
                        })
                        .take(request.getLimit())
                        ;
                }
                return operations
                    .query(dataSql)
                    .map(timeSeriesData -> {
                        long ts = timeSeriesData.getTimestamp();
                        Map<String, Object> newData = timeSeriesData.getData();
                        newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())));
                        newData.put("_time", ts);
                        return newData;
                    })
                    .groupBy(data -> (String) data.get("time"), Integer.MAX_VALUE)
                    .flatMap(group -> group
                        .reduceWith(HashMap::new, (a, b) -> {
                            a.putAll(b);
                            return a;
                        })
                        .map(map -> {
                            Map<String, Object> newResult = new HashMap<>();
                            for (DeviceDataService.DevicePropertyAggregation property : properties) {
                                String key = "value_" + property.getAlias();
                                newResult.put(property.getAlias(), Optional.ofNullable(map.get(key)).orElse(0));
                            }
                            newResult.put("time", group.key());
                            newResult.put("_time", map.getOrDefault("_time", new Date()));
                            return AggregationData.of(newResult);
                        }))
                    .sort(Comparator.<AggregationData, Date>comparing(data -> CastUtils.castDate(data
                                                                                                     .values()
                                                                                                     .get("_time"))).reversed())
                    .doOnNext(data -> data.values().remove("_time"))
                    .take(request.getLimit());

            });
    }
}
