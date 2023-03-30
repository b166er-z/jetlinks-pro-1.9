package org.jetlinks.pro.device.service.data;

import io.netty.util.Recycler;
import io.r2dbc.spi.R2dbcBadGrammarException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.ValueCodec;
import org.hswebframework.ezorm.rdb.codec.ClobValueCodec;
import org.hswebframework.ezorm.rdb.codec.DateTimeCodec;
import org.hswebframework.ezorm.rdb.codec.JsonValueCodec;
import org.hswebframework.ezorm.rdb.codec.NumberValueCodec;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.record.Record;
import org.hswebframework.ezorm.rdb.metadata.JdbcDataType;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.ddl.TableBuilder;
import org.hswebframework.ezorm.rdb.operator.dml.SelectColumnSupplier;
import org.hswebframework.ezorm.rdb.operator.dml.query.Selects;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.pro.ConfigMetadataConstants;
import org.jetlinks.pro.device.entity.DeviceLatestData;
import org.jetlinks.pro.elastic.search.service.reactive.ReactiveElasticSearchService;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationColumn;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.math.MathFlux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.sql.JDBCType;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备最新数据服务,用于保存设备最新的相关数据到关系型数据库中，可以使用动态条件进行查询相关数据
 *
 * @author zhouhao
 * @since 1.5.0
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "jetlinks.device.storage.latest")
public class DatabaseDeviceLatestDataService implements DeviceLatestDataService {

    private FluxSink<Buffer> bufferSink;

    private final DatabaseOperator databaseOperator;

    @Getter
    @Setter
    private int bufferSize = 500;

    @Getter
    @Setter
    private int backpressureBuffer = 10;

    @Getter
    @Setter
    private Duration bufferTimeout = Duration.ofSeconds(2);


    public DatabaseDeviceLatestDataService(DatabaseOperator databaseOperator) {
        this.databaseOperator = databaseOperator;
        init();
    }

    public static String getLatestTableTableName(String productId) {
        return "dev_lst_" + (productId.toLowerCase().replace("-", "_").replace(".", "_"));
    }

    private String getEventColumn(String event, String property) {
        return event + "_" + property;
    }

    public void init() {

        Flux.<Buffer>create(sink -> bufferSink = sink)
            //按表分组
            .groupBy(Buffer::getTable)
            .flatMap(group -> {
                AtomicLong sizeCount = new AtomicLong();
                String table = group.key();
                return FluxUtils
                    .bufferRate(group, 1000, bufferSize, bufferTimeout, (buf, all) -> sizeCount.addAndGet(buf.size()) >= 2000)
                    .doOnNext(ignore -> sizeCount.set(0))
                    //背压丢弃处理不过来的数据,防止内存溢出
                    .onBackpressureBuffer(backpressureBuffer,
                                          drop -> {
                                              drop.forEach(Buffer::release);
                                              System.err.println("无法处理更多的设备[" + table + "]最新数据同步!");
                                          },
                                          BufferOverflowStrategy.DROP_OLDEST)
                    .publishOn(Schedulers.boundedElastic(), backpressureBuffer)
                    //concatMap 串行执行更新,防止死锁
                    .concatMap(list -> Flux
                        .fromIterable(list)
                        //同一个设备的属性合并在一起
                        .groupBy(Buffer::getDeviceId, Integer.MAX_VALUE)
                        .flatMap(sameDevice -> sameDevice.reduce(Buffer::merge))
                        .collectList()
                        //批量更新
                        .flatMap(sameTableData -> {
                            try {
                                Buffer first = sameTableData.get(0);
                                List<Map<String, Object>> data = sameTableData
                                    .stream()
                                    .map(Buffer::getProperties)
                                    .collect(Collectors.toList());
                                return this
                                    .doUpdateLatestData(first.table, data)
                                    .onErrorResume((err) -> {
                                        log.error("save device latest data error", err);
                                        return Mono.empty();
                                    });
                            } finally {
                                sameTableData.forEach(Buffer::release);
                            }
                        })
                    )
                    .onErrorContinue((err, res) -> {
                        log.error("save device latest data error", err);
                    });
            }, Integer.MAX_VALUE)
            .subscribe();
    }

    static GeoCodec geoCodec = new GeoCodec();

    static StringCodec stringCodec = new StringCodec();

    static class GeoCodec implements ValueCodec<String, GeoPoint> {

        @Override
        public String encode(Object value) {
            return String.valueOf(value);
        }

        @Override
        public GeoPoint decode(Object data) {
            return GeoPoint.of(data);
        }
    }

    static class StringCodec implements ValueCodec<String, String> {

        @Override
        public String encode(Object value) {
            return String.valueOf(value);
        }

        @Override
        public String decode(Object data) {
            return String.valueOf(data);
        }
    }

    private RDBColumnMetadata convertColumn(PropertyMetadata metadata) {
        RDBColumnMetadata column = new RDBColumnMetadata();
        column.setName(metadata.getId());
        column.setComment(metadata.getName());
        DataType type = metadata.getValueType();
        if (type instanceof NumberType) {
            column.setLength(32);
            column.setPrecision(32);
            if (type instanceof DoubleType) {
                column.setScale(Optional.ofNullable(((DoubleType) type).getScale()).orElse(2));
                column.setValueCodec(new NumberValueCodec(Double.class));
                column.setJdbcType(JDBCType.NUMERIC, Double.class);
            } else if (type instanceof FloatType) {
                column.setScale(Optional.ofNullable(((FloatType) type).getScale()).orElse(2));
                column.setValueCodec(new NumberValueCodec(Float.class));
                column.setJdbcType(JDBCType.NUMERIC, Float.class);
            } else if (type instanceof LongType) {
                column.setValueCodec(new NumberValueCodec(Long.class));
                column.setJdbcType(JDBCType.NUMERIC, Long.class);
            } else {
                column.setValueCodec(new NumberValueCodec(IntType.class));
                column.setJdbcType(JDBCType.NUMERIC, Integer.class);
            }
        } else if (type instanceof ObjectType) {
            column.setJdbcType(JDBCType.CLOB, String.class);
            column.setValueCodec(JsonValueCodec.of(Map.class));
        } else if (type instanceof ArrayType) {
            column.setJdbcType(JDBCType.CLOB, String.class);
            column.setValueCodec(JsonValueCodec.ofCollection(ArrayList.class, Map.class));
        } else if (type instanceof DateTimeType) {
            column.setJdbcType(JDBCType.TIMESTAMP, Long.class);
            String format = ((DateTimeType) type).getFormat();
            if (DateTimeType.TIMESTAMP_FORMAT.equals(format)) {
                format = "yyyy-MM-dd HH:mm:ss";
            }
            column.setValueCodec(new DateTimeCodec(format, Long.class));
        } else if (type instanceof GeoType) {
            column.setJdbcType(JDBCType.VARCHAR, String.class);
            column.setValueCodec(geoCodec);
            column.setLength(128);
        } else if (type instanceof EnumType) {
            column.setJdbcType(JDBCType.VARCHAR, String.class);
            column.setValueCodec(stringCodec);
            column.setLength(64);
        } else {
            int len = type
                .getExpand(ConfigMetadataConstants.maxLength.getKey())
                .filter(o->!StringUtils.isEmpty(o))
                .map(CastUtils::castNumber)
                .map(Number::intValue)
                .orElse(255);
            if (len > 2048) {
                column.setJdbcType(JDBCType.CLOB, String.class);
                column.setValueCodec(ClobValueCodec.INSTANCE);
            } else {
                column.setJdbcType(JDBCType.VARCHAR, String.class);
                column.setLength(len);
                column.setValueCodec(stringCodec);
            }
        }

        return column;
    }


    public Mono<Void> reloadMetadata(String productId, DeviceMetadata metadata) {
        return Mono
            .defer(() -> {
                String tableName = getLatestTableTableName(productId);
                log.debug("reload product[{}] metadata,table name:[{}] ", productId, tableName);
                RDBSchemaMetadata schema = databaseOperator.getMetadata()
                                                           .getCurrentSchema();

                RDBTableMetadata table = schema.newTable(tableName);

                RDBColumnMetadata id = table.newColumn();
                id.setName("id");
                id.setLength(64);
                id.setPrimaryKey(true);
                id.setJdbcType(JDBCType.VARCHAR,String.class);
                table.addColumn(id);

                RDBColumnMetadata deviceName = table.newColumn();
                deviceName.setLength(128);
                deviceName.setName("device_name");
                deviceName.setAlias("deviceName");
                deviceName.setJdbcType(JDBCType.VARCHAR,String.class);
                table.addColumn(deviceName);

                for (PropertyMetadata property : metadata.getProperties()) {
                    table.addColumn(convertColumn(property));
                }
                for (EventMetadata event : metadata.getEvents()) {
                    DataType type = event.getType();
                    if (type instanceof ObjectType) {
                        for (PropertyMetadata property : ((ObjectType) type).getProperties()) {
                            RDBColumnMetadata column = convertColumn(property);
                            column.setName(getEventColumn(event.getId(), property.getId()));
                            table.addColumn(column);
                        }
                    }
                }

                return schema
                    .getTableReactive(tableName, false)
                    .doOnNext(oldTable -> oldTable.replace(table))
                    .switchIfEmpty(Mono.fromRunnable(() -> schema.addTable(table)))
                    .then();
            });
    }

    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> upgradeMetadata(String productId, DeviceMetadata metadata, boolean ddl) {
        return Mono
            .defer(() -> {
                String tableName = getLatestTableTableName(productId);
                log.debug("upgrade product[{}] metadata,table name:[{}] ", productId, tableName);
                RDBTableMetadata table = databaseOperator
                    .getMetadata()
                    .getCurrentSchema()
                    .newTable(tableName);

                TableBuilder builder = databaseOperator
                    .ddl()
                    .createOrAlter(table)
                    .addColumn("id").primaryKey().varchar(64).commit()
                    .addColumn("device_name").alias("deviceName").varchar(128).notNull().commit()
                    .merge(false)
                    .allowAlter(ddl);

                for (PropertyMetadata property : metadata.getProperties()) {
                    builder.addColumn(convertColumn(property));
                }
                for (EventMetadata event : metadata.getEvents()) {
                    DataType type = event.getType();
                    if (type instanceof ObjectType) {
                        for (PropertyMetadata property : ((ObjectType) type).getProperties()) {
                            RDBColumnMetadata column = convertColumn(property);
                            column.setName(getEventColumn(event.getId(), property.getId()));
                            builder.addColumn(column);
                        }
                    }
                }
                return builder
                    .commit()
                    .reactive()
                    .subscribeOn(Schedulers.elastic())
                    .then();
            });
    }

    public Mono<Void> upgradeMetadata(String productId, DeviceMetadata metadata) {
        return upgradeMetadata(productId, metadata, true);
    }

    public void save(DeviceMessage message) {
        try {
            Map<String, Object> properties = DeviceMessageUtils
                .tryGetProperties(message)
                .orElseGet(() -> {
                    //事件
                    if (message instanceof EventMessage) {
                        Object data = ((EventMessage) message).getData();
                        String event = ((EventMessage) message).getEvent();
                        if (data instanceof Map) {
                            Map<?, ?> mapValue = (Map<?, ?>) data;
                            Map<String, Object> val = new HashMap<>(mapValue
                                                                        .size());
                            ((Map<?, ?>) data).forEach((k, v) -> {
                                val.put(getEventColumn(event, String.valueOf(k)), v);
                            });
                            return val;
                        }
                        return Collections.singletonMap(getEventColumn(event, "value"), data);
                    }
                    return null;
                });
            if (CollectionUtils.isEmpty(properties)) {
                return;
            }
            String productId = message.getHeader("productId").map(String::valueOf).orElse("null");
            String deviceName = message.getHeader("deviceName").map(String::valueOf).orElse(message.getDeviceId());
            String tableName = getLatestTableTableName(productId);
            Map<String, Object> prob = new HashMap<>(properties);
            prob.put("id", message.getDeviceId());
            prob.put("deviceName", deviceName);

            Buffer buffer = Buffer.of(tableName, message.getDeviceId(), deviceName, prob, message.getTimestamp());
            bufferSink.next(buffer);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    static Recycler<Buffer> RECYCLER = new Recycler<Buffer>() {
        @Override
        protected Buffer newObject(Handle<Buffer> handle) {
            return new Buffer(handle);
        }
    };

    @AllArgsConstructor(staticName = "of")
    @Getter
    private static class Buffer {
        private String table;

        private String deviceId;

        private String deviceName;

        private Map<String, Object> properties;

        private long timestamp;

        private final Recycler.Handle<Buffer> handle;

        public Buffer(Recycler.Handle<Buffer> handle) {
            this.handle = handle;
        }

        public static Buffer of(String table, String deviceId, String deviceName, Map<String, Object> properties, long timestamp) {
            Buffer buffer;
            try {
                buffer = RECYCLER.get();
            } catch (Exception e) {
                buffer = new Buffer(null);
            }
            buffer.table = table;
            buffer.deviceId = deviceId;
            buffer.deviceName = deviceName;
            buffer.properties = properties;
            buffer.timestamp = timestamp;
            return buffer;
        }

        void release() {
            this.table = null;
            this.deviceId = null;
            this.deviceName = null;
            this.properties = null;
            this.timestamp = 0;
            if (null != handle) {
                handle.recycle(this);
            }
        }

        public Buffer merge(Buffer buffer) {

            //以比较新的数据为准
            if (buffer.timestamp > this.timestamp) {
                return buffer.merge(this);
            }
            //合并
            buffer.properties.forEach(properties::putIfAbsent);
            buffer.release();
            return this;
        }

        int size() {
            return properties == null ? 0 : properties.size();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> doUpdateLatestData(String table,
                                         List<Map<String, Object>> properties) {
        return databaseOperator
            .getMetadata()
            .getTableReactive(table)
            .flatMap(ignore -> databaseOperator
                .dml()
                .upsert(table)
                .ignoreUpdate("id")
                .values(properties)
                .execute()
                .reactive()
                .then());
    }

    public ReactiveRepository<Record, String> getRepository(String productId) {
        return databaseOperator
            .dml()
            .createReactiveRepository(getLatestTableTableName(productId));
    }

    @Override
    public Flux<DeviceLatestData> query(String productId, QueryParamEntity param) {
        return getRepository(productId)
            .createQuery()
            .setParam(param)
            .fetch()
            .map(DeviceLatestData::new);
    }

    @Override
    public Mono<DeviceLatestData> queryDeviceData(String productId, String deviceId) {
        return getRepository(productId)
            .findById(deviceId)
            .map(DeviceLatestData::new);
    }

    @Override
    public Mono<Integer> count(String productId, QueryParamEntity param) {
        return getRepository(productId)
            .createQuery()
            .setParam(param)
            .count();
    }

    private SelectColumnSupplier createAggColumn(AggregationColumn column) {
        switch (column.getAggregation()) {
            case COUNT:
                return Selects.count(column.getProperty()).as(column.getAlias());
            case AVG:
                return Selects.avg(column.getProperty()).as(column.getAlias());
            case MAX:
                return Selects.max(column.getProperty()).as(column.getAlias());
            case MIN:
                return Selects.min(column.getProperty()).as(column.getAlias());
            case SUM:
                return Selects.sum(column.getProperty()).as(column.getAlias());
            default:
                throw new UnsupportedOperationException("unsupported agg:" + column.getAggregation());
        }
    }

    private SelectColumnSupplier[] createAggColumns(List<AggregationColumn> columns) {
        return columns
            .stream()
            .map(this::createAggColumn)
            .toArray(SelectColumnSupplier[]::new);
    }

    @Override
    public Mono<Map<String, Object>> aggregation(String productId,
                                                 List<AggregationColumn> columns,
                                                 QueryParamEntity paramEntity) {
        if (CollectionUtils.isEmpty(columns)) {
            return Mono.error(new ValidationException("columns", "聚合列不能为空"));
        }
        String table = getLatestTableTableName(productId);

        return databaseOperator
            .getMetadata()
            .getTableReactive(table)
            .flatMap(tableMetadata ->
                     {
                         List<String> illegals = new ArrayList<>();

                         List<AggregationColumn> columnList = columns.stream()
                                                                     .filter(column -> {
                                                                         if (tableMetadata
                                                                             .getColumn(column.getProperty())
                                                                             .isPresent()) {
                                                                             return true;
                                                                         }
                                                                         illegals.add(column.getProperty());
                                                                         return false;
                                                                     })
                                                                     .collect(Collectors.toList());
                         if (CollectionUtils.isEmpty(columnList)) {
                             return Mono.error(new ValidationException("columns", "无效的产品[" + productId + "]属性或事件:" + illegals));
                         }
                         return databaseOperator
                             .dml()
                             .query(table)
                             .select(createAggColumns(columnList))
                             .setParam(paramEntity.clone().noPaging())
                             .fetch(ResultWrappers.map())
                             .reactive()
                             .take(1)
                             .singleOrEmpty()
                             .doOnNext(map -> {
                                 for (AggregationColumn column : columns) {
                                     map.putIfAbsent(column.getAlias(), 0);
                                 }
                             })
                             //表不存在
                             .onErrorReturn(e -> StringUtils.hasText(e.getMessage()) && e
                                 .getMessage()
                                 .contains("doesn't exist "), Collections.emptyMap());
                     }
            );

    }

    @Override
    public Flux<Map<String, Object>> aggregation(Flux<QueryProductLatestDataRequest> param,
                                                 boolean merge) {
        Flux<QueryProductLatestDataRequest> cached = param.cache();
        return cached
            .flatMap(request -> this
                .aggregation(request.getProductId(), request.getColumns(), request.getQuery())
                .doOnNext(map -> {
                    if (!merge) {
                        map.put("productId", request.getProductId());
                    }
                }))
            .as(flux -> {
                if (!merge) {
                    return flux;
                }
                //合并所有产品的字段到一条数据中,合并时,使用第一个聚合字段使用的聚合类型
                return cached
                    .take(1)
                    .flatMapIterable(QueryLatestDataRequest::getColumns)
                    .collectMap(AggregationColumn::getAlias, agg -> aggMappers.getOrDefault(agg.getAggregation(), sum))
                    .flatMap(mappers -> flux
                        .flatMapIterable(Map::entrySet)
                        .groupBy(Map.Entry::getKey,Integer.MAX_VALUE)
                        .flatMap(group -> mappers
                            .getOrDefault(group.key(), sum)
                            .apply(group.map(Map.Entry::getValue))
                            .map(val -> Tuples.of(String.valueOf(group.key()), (Object) val)))
                        .collectMap(Tuple2::getT1, Tuple2::getT2)).flux();
            });
    }


    static Map<Aggregation, Function<Flux<Object>, Mono<? extends Number>>> aggMappers = new HashMap<>();

    static Function<Flux<Object>, Mono<? extends Number>> avg = flux -> MathFlux.averageDouble(flux
                                                                                                   .map(CastUtils::castNumber)
                                                                                                   .map(Number::doubleValue));
    static Function<Flux<Object>, Mono<? extends Number>> max = flux -> MathFlux.max(flux
                                                                                         .map(CastUtils::castNumber)
                                                                                         .map(Number::doubleValue));
    static Function<Flux<Object>, Mono<? extends Number>> min = flux -> MathFlux.min(flux
                                                                                         .map(CastUtils::castNumber)
                                                                                         .map(Number::doubleValue));
    static Function<Flux<Object>, Mono<? extends Number>> sum = flux -> MathFlux.sumDouble(flux
                                                                                               .map(CastUtils::castNumber)
                                                                                               .map(Number::doubleValue));

    static {
        aggMappers.put(Aggregation.AVG, avg);
        aggMappers.put(Aggregation.MAX, max);
        aggMappers.put(Aggregation.MIN, min);
        aggMappers.put(Aggregation.SUM, sum);
        aggMappers.put(Aggregation.COUNT, sum);
    }

}
