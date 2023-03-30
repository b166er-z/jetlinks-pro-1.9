package org.jetlinks.pro.tdengine;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.pro.ConfigMetadataConstants;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.timeseries.TimeSeriesData;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DefaultTDengineRepository implements TDengineRepository {

    private final TDengineProperties properties;

    private final TDengineOperations operations;

    private final Map<String, TDengineSTableMetadata> metricMetadata = new ConcurrentHashMap<>();

    private FluxSink<Point> sink;

    @Setter
    private int maxRetry = 3;

    public DefaultTDengineRepository(TDengineProperties properties, TDengineOperations operations) {
        this.properties = properties;
        this.operations = operations;
        init();
    }

    private void init() {

        FluxUtils
            .<Point>bufferRate(
                Flux.create(sink -> this.sink = sink),
                properties.getRestful().getBufferSize(),
                properties.getRestful().getBufferRate(),
                properties.getRestful().getBufferTimeout()
            )
            .onBackpressureBuffer(properties.getRestful().getBackpressureBufferSize(),
                                  list -> log.warn("无法处理更多TDEngine写入请求"),
                                  BufferOverflowStrategy.DROP_OLDEST)
            .publishOn(Schedulers.boundedElastic(), properties.getRestful().getBackpressureBufferSize())
            .flatMap(batch -> {
                AtomicLong counter = new AtomicLong();
                long maxBytes = properties.getRestful().getMaxBufferBytes().toBytes();

                return this
                    //生成SQL
                    .generateInsertSql(batch)
                    //判断最大缓冲字节数
                    .bufferUntil(sql -> {
                        if (counter.addAndGet(sql.length() * 2L) >= maxBytes) {
                            counter.set(0);
                            return true;
                        }
                        return false;
                    }, false)
                    .flatMap(sqlList -> operations
                        .execute(sqlList.stream().collect(Collectors.joining("\n", "insert into ", "")))
                        .retryWhen(Retry
                                       .backoff(properties
                                                    .getRestful()
                                                    .getMaxRetry(), Duration.ofSeconds(1))
                                       .filter(error -> !(error instanceof TDengineException)))
                        .onErrorResume((err) -> {
                            log.error("保存TDengine数据失败", err);
                            return Mono.empty();
                        })
                    )
                    .onErrorResume((err) -> {
                        log.error("保存TDengine数据失败", err);
                        return Mono.empty();
                    })
                    ;
            }, Integer.MAX_VALUE)
            .onErrorContinue((err, val) -> log.error("保存TDengine数据失败", err))
            .subscribe();
    }

    public void shutdown() {
        sink.complete();
    }

    @Override
    public String getTimestampColumn() {
        return "_ts";
    }

    @Override
    public String getDatabase() {
        return properties.getDatabase();
    }

    @Override
    public String getTableForQuery(String tableName) {
        return createFullTableName(tableName);
    }

    @Override
    public String convertColumnName(String column) {
        return convertSafeColumn(column);
    }


    protected static String convertSafeColumn(String column) {
        return column.toLowerCase();
    }

    @Getter
    @Setter
    static class TDengineSTableMetadata {

        private Map<String, PropertyMetadata> columns = new CaseInsensitiveMap<>();
        private Map<String, PropertyMetadata> tags = new CaseInsensitiveMap<>();

        private String columnString;

        private void initColumnString() {
            columnString = columns
                .keySet()
                .stream()
                .map(DefaultTDengineRepository::convertSafeColumn)
                .collect(Collectors.joining("\",\"", "\"", "\""));
        }

        private void columns(Collection<PropertyMetadata> columns) {
            if (columns == null) {
                return;
            }
            for (PropertyMetadata column : columns) {
                if ("_ts".equals(column.getId()) || "timestamp".equals(column.getId())) {
                    continue;
                }
                this.columns.put(column.getId(), column);
            }
        }

        private void tags(Collection<PropertyMetadata> columns) {
            if (columns == null) {
                return;
            }
            for (PropertyMetadata column : columns) {
                this.tags.put(column.getId(), column);
            }
        }

        private String getColumnString() {
            if (columnString == null) {
                initColumnString();
            }
            return columnString;
        }

        private TDengineSTableMetadata merge(TDengineSTableMetadata metadata) {
            TDengineSTableMetadata newMetadata = new TDengineSTableMetadata();
            newMetadata.columns.putAll(this.columns);
            newMetadata.tags.putAll(this.tags);
            newMetadata.columns.putAll(metadata.columns);
            newMetadata.tags.putAll(metadata.tags);
            return newMetadata;
        }

        private boolean isEmpty() {
            return MapUtils.isEmpty(columns) && MapUtils.isEmpty(tags);
        }

        private boolean isNotEmpty() {
            return !isEmpty();
        }

        public Flux<String> generateAlterSQL(String tableName) {
            return Flux
                .concat(
                    Flux
                        .fromIterable(columns.values())
                        .map(prop -> String.join(" ", "ALTER TABLE", tableName,
                                                 "ADD COLUMN ",
                                                 "\"" + convertSafeColumn(prop.getId()) + "\"",
                                                 convertColumnType(prop.getValueType())))
                    ,
                    Flux
                        .fromIterable(tags.values())
                        .map(tag -> String.join(" ",
                                                "ALTER TABLE", tableName,
                                                "ADD TAG",
                                                "\"" + convertSafeColumn(tag.getId()) + "\"",
                                                convertColumnType(tag.getValueType())))
                );
        }

        public Flux<String> generateCreateSQL(String tableName) {
            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS \n")
                .append(tableName)
                .append("\n(_ts timestamp");

            for (Map.Entry<String, PropertyMetadata> column : columns.entrySet()) {
                sql.append(",\"")
                   .append(convertSafeColumn(column.getKey()))
                   .append("\" ")
                   .append(convertColumnType(column.getValue().getValueType()));
            }
            sql.append(")");

            if (!tags.isEmpty()) {
                sql.append("\nTAGS\n(");
                int idx = 0;
                for (Map.Entry<String, PropertyMetadata> tag : tags.entrySet()) {
                    if (idx++ > 0) {
                        sql.append(",");
                    }
                    sql.append("\"")
                       .append(convertSafeColumn(tag.getKey()))
                       .append("\" ")
                       .append(convertColumnType(tag.getValue().getValueType()));
                }
                sql.append(")");
            }

            return Flux.just(sql.toString());
        }

        private String generateSqlValue(Object value, DataType type) {
            if (value == null) {
                return "null";
            }

            if (value instanceof Date) {
                return String.valueOf(((Date) value).getTime());
            }

            if (value instanceof Number || type instanceof NumberType || type instanceof BooleanType) {
                return String.valueOf(value);
            }
            if (type instanceof DateTimeType) {
                return String.valueOf(((DateTimeType) type).convert(value).getTime());
            }

            return "'" + (String.valueOf(value).replace("'", "\\'")) + "'";
        }


        public String generateInsert(Collection<Point> points) {
            StringBuilder joiner = new StringBuilder(points.size() * 64);

            if (points.isEmpty()) {
                return "";
            }
            if (MapUtils.isNotEmpty(tags)) {
                Point first = points.iterator().next();
                if (MapUtils.isNotEmpty(first.getTags())) {
                    joiner.append("tags(");
                    int idx = 0;
                    Map<String, Object> values = new HashMap<>();
                    first.getTags().forEach((k, v) -> values.put(convertSafeColumn(k), v));
                    for (Map.Entry<String, PropertyMetadata> entry : tags.entrySet()) {
                        Object value = values.get(entry.getKey());
                        if (idx++ > 0) {
                            joiner.append(",");
                        }
                        joiner.append(generateSqlValue(value, entry.getValue().getValueType()));
                    }
                    joiner.append(")");
                }

            }
            joiner.append("(_ts,");
            joiner.append(getColumnString());
            joiner.append(")");
            joiner.append("values");
            for (Point point : points) {
                joiner.append("(");
                joiner.append(point.getTimestamp() == 0 ? System.currentTimeMillis() : point.getTimestamp());

                Map<String, Object> values = new HashMap<>();
                point.getValues().forEach((k, v) -> values.put(convertSafeColumn(k), v));
                for (Map.Entry<String, PropertyMetadata> entry : columns.entrySet()) {
                    Object value = values.get(entry.getKey());
                    joiner.append(",").append(generateSqlValue(value, entry.getValue().getValueType()));
                }
                joiner.append(")");
            }

            return joiner.toString();

        }
    }

    //对比表结构,并返回新增的列或者标签
    private TDengineSTableMetadata compareMetadata(TDengineSTableMetadata newer, TDengineSTableMetadata older) {
        TDengineSTableMetadata metadata = new TDengineSTableMetadata();
        metadata.columns.putAll(newer.columns);
        metadata.tags.putAll(newer.tags);

        older.columns.remove("_ts");
        older.columns.keySet().forEach(metadata.columns::remove);
        older.tags.keySet().forEach(metadata.tags::remove);

        return metadata;
    }

    private String createFullTableName(String tableName) {
        return String.join(".", properties.getDatabase(), tableName.replace("-", "_").replace(".", "_").toLowerCase());
    }

    private PropertyMetadata convertColumn(Map<String, Object> metadata) {
        ValueObject values = ValueObject.of(metadata);

        SimplePropertyMetadata column = new SimplePropertyMetadata();

        String field = values.getString("Field").orElseThrow(() -> new NullPointerException("Field value is null"));
        String type = values.getString("Type").orElseThrow(() -> new NullPointerException("Type value is null"));

        column.setId(field);
        column.setName(field);
        column.setValueType(convertColumnType(type));

        return column;
    }

    private static String convertColumnType(DataType type) {
        if (type instanceof FloatType) {
            return "float";
        }
        if (type instanceof LongType) {
            return "bigint";
        }
        if (type instanceof BooleanType) {
            return "bool";
        }
        if (type instanceof DoubleType) {
            return "double";
        }
        if (type instanceof IntType) {
            return "int";
        }
        if (type instanceof StringType) {
            return "binary(" + type.getExpand(ConfigMetadataConstants.maxLength.getKey()).orElse(256) + ")";
        }
        //tdengine 只支持一列时间戳类型
        if (type instanceof DateTimeType) {
            return "bigint";
        }

        return "binary(" + type.getExpand(ConfigMetadataConstants.maxLength.getKey()).orElse(1024) + ")";
    }

    private static DataType convertColumnType(String type) {
        switch (type.toLowerCase()) {
            case "float":
                return new FloatType();
            case "bigint":
                return LongType.GLOBAL;
            case "bool":
                return BooleanType.GLOBAL;
            case "smallint":
            case "int":
                return IntType.GLOBAL;
            case "double":
                return DoubleType.GLOBAL;
            case "binary":
            case "nchar":
                return StringType.GLOBAL;
            case "timestamp":
                return DateTimeType.GLOBAL;
            default:
                throw new UnsupportedOperationException("unsupported type:" + type);
        }
    }

    private Mono<TDengineSTableMetadata> getStable(String metric) {
        return Mono.justOrEmpty(metricMetadata.get(metric))
                   .switchIfEmpty(Mono.defer(() -> this
                       .getStableFromDb(metric)
                       .doOnNext(metadata -> metricMetadata.putIfAbsent(metric, metadata)))
                   );
    }

    private Mono<TDengineSTableMetadata> getStableFromDb(String table) {
        String sql = "describe " + createFullTableName(table);
        return operations
            .query(sql)
            .groupBy(map -> "TAG".equals(map.get("Note")))
            .flatMap(group -> Mono
                .zip(
                    Mono.just(Boolean.TRUE.equals(group.key())),
                    group.map(this::convertColumn).collectList()
                ))
            .reduceWith(TDengineSTableMetadata::new, (a, v) -> {
                if (v.getT1()) {
                    a.tags(v.getT2());
                } else {
                    a.columns(v.getT2());
                }
                return a;
            })
            .filter(TDengineSTableMetadata::isNotEmpty)
            .onErrorResume(err -> Mono.empty());
    }

    private TDengineSTableMetadata convertToStable(TimeSeriesMetadata metadata,
                                                   Set<String> tags) {
        Map<Boolean, List<PropertyMetadata>> groups = metadata
            .getProperties()
            .stream()
            .collect(Collectors.groupingBy(property -> tags.contains(property.getId())));

        TDengineSTableMetadata meta = new TDengineSTableMetadata();
        meta.columns(groups.get(false));
        meta.tags(groups.get(true));

        return meta;
    }

    @Override
    public Mono<Void> registerMetadata(TimeSeriesMetadata metadata,
                                       Set<String> tags,
                                       boolean ddl) {
        //超级表
        TDengineSTableMetadata stable = convertToStable(metadata, tags);

        String tableName = createFullTableName(metadata.getMetric().getId());

        return this
            .getStableFromDb(metadata.getMetric().getId())
            .map(inDB -> {
                TDengineSTableMetadata diff = compareMetadata(stable, inDB);
                if (diff.isEmpty()) {
                    return Mono.<TDengineSTableMetadata>empty();
                }
                if (!ddl) {
                    return Mono.just(stable.merge(inDB));
                }
                return diff
                    .generateAlterSQL(tableName)
                    .concatMap(operations::execute)
                    .then(Mono.just(stable.merge(inDB)));
            })
            .switchIfEmpty(Mono.fromSupplier(() -> stable
                .generateCreateSQL(tableName)
                .concatMap(operations::execute)
                .then(Mono.just(stable))))
            .flatMap(Function.identity())
            .doOnNext(newTable -> metricMetadata.put(metadata.getMetric().getId(), newTable))
            .then();

    }

    @Override
    public Mono<Void> insert(Point data) {
        sink.next(data);
        return Mono.empty();
    }

    public Flux<String> generateInsertSql(Collection<Point> batch) {
        return Flux
            .fromIterable(batch)
            .groupBy(Point::getMetric,Integer.MAX_VALUE)
            .flatMap(group -> {
                String metric = String.valueOf(group.key());
                String stableName = createFullTableName(metric);
                return getStable(metric)
                    .flatMapMany(stable -> group
                        .groupBy(Point::getTable,Integer.MAX_VALUE)
                        .flatMap(tableGroup -> {
                            String key = String.valueOf(tableGroup.key());
                            String table = key.contains(".") ? key : createFullTableName(key);
                            return tableGroup
                                .collectList()
                                .map(points -> String.join(" ", table, "using", stableName, stable.generateInsert(points)));
                        }));
            });
    }

    @Override
    public Mono<Void> insert(Collection<Point> batch) {
        return this
            .generateInsertSql(batch)
            .collect(Collectors.joining("\n", "insert into ", ""))
            .flatMap(operations::execute);
    }

    private static final DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Flux<TimeSeriesData> query(String sql) {

        return operations
            .query(sql)
            .map(map -> {
                String ts = (String) map.remove("_ts");
                long timestamp = 0;
                if (ts == null) {
                    ts = (String) map.remove("ts");
                }
                if (ts != null) {
                    timestamp = DateTime.parse(ts, format).getMillis();
                }
                map.put("timestamp", timestamp);
                return TimeSeriesData.of(timestamp, map);
            });
    }
}
