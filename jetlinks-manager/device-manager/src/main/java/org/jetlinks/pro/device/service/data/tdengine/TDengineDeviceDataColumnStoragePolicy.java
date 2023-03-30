package org.jetlinks.pro.device.service.data.tdengine;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.device.service.data.AbstractDeviceDataStoragePolicy;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.service.data.DeviceDataStorageProperties;
import org.jetlinks.pro.device.measurements.message.timeseries.DeviceTimeSeriesMetadata;
import org.jetlinks.pro.tdengine.Point;
import org.jetlinks.pro.tdengine.TDengineRepository;
import org.jetlinks.pro.tdengine.term.TDengineQueryConditionBuilder;
import org.jetlinks.pro.tdengine.term.TDengineTermType;
import org.jetlinks.pro.timeseries.TimeSeriesData;
import org.jetlinks.pro.timeseries.TimeSeriesMetadata;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 使用TDengine存储设备属性以及事件数据.
 *
 * @author zhouhao
 * @since 1.5
 */
@Slf4j
public class TDengineDeviceDataColumnStoragePolicy extends AbstractDeviceDataStoragePolicy {


    private final TDengineRepository repository;

    public TDengineDeviceDataColumnStoragePolicy(DeviceRegistry registry,
                                                 TDengineRepository repository,
                                                 DeviceDataStorageProperties properties) {
        super(registry, properties);
        this.repository = repository;
    }


    @Override
    public String getId() {
        return "tdengine-column";
    }

    @Override
    public String getName() {
        return "TDengine-列式存储";
    }

    @Override
    public String getDescription() {
        return "每个设备的全部属性为一行数据.需要设备每次上报全部属性.";
    }

    @Nonnull
    @Override
    public Mono<ConfigMetadata> getConfigMetadata() {
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doSaveData(String metric, TimeSeriesData data) {
        return repository.insert(convertDataToPoint(metric, data));
    }

    @Override
    protected Mono<Void> doSaveData(String metric, Flux<TimeSeriesData> dataStream) {
        return dataStream
            .map(data -> convertDataToPoint(metric, data))
            .collectList()
            .flatMap(repository::insert);
    }

    private String getChildTable(String stable, String deviceId) {
        return repository.getTableForQuery(stable + "_" + deviceId);
    }

    protected Point convertDataToPoint(String metric, TimeSeriesData data) {
        String deviceId = data.getString("deviceId").orElse("null");
        Point point = Point.of(metric, getChildTable(metric, deviceId));

        point.tag("deviceId", deviceId);
        point.values(data.values());
        point.timestamp(data.getTimestamp());
        return point;
    }

    @Override
    protected Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId,
                                                                     DeviceMessage message,
                                                                     Map<String, Object> properties) {
        return convertPropertiesForColumnPolicy(productId, message, properties);
    }


    protected String buildOrderBy(QueryParamEntity param) {

        for (Sort sort : param.getSorts()) {
            if (sort.getName().toLowerCase().contains("time")) {
                return " order by " + repository.getTimestampColumn() + " " + sort.getOrder();
            }
        }
        return " order by " + repository.getTimestampColumn() + " desc";
    }


    //预处理查询条件
    protected List<Term> prepareTerms(String table, List<Term> terms) {

        if (CollectionUtils.isEmpty(terms)) {
            return terms;
        }
        for (Term term : terms) {
            this.getMetricColumn(table, term.getColumn())
                .ifPresent(meta -> {
                    DataType type = meta.getValueType();
                    if (type instanceof DateTimeType) {
                        //处理时间类型,tdengine只支持第一个列为时间戳类型,其他时间类型的字段都存储为时间戳bigint
                        //因此这里将时间类型转换为时间戳数值
                        Collection<Object> values = TDengineTermType.convertList(term.getValue());
                        Iterator<Object> iterator = values.iterator();
                        if (values.size() == 1) {
                            term.setValue(CastUtils.castDate(iterator.next()).getTime());
                        }
                        //between and的情况
                        if (values.size() == 2) {
                            term.setValue(Arrays.asList(
                                CastUtils.castDate(iterator.next()).getTime(),
                                CastUtils.castDate(iterator.next()).getTime()
                            ));
                        }
                        return;
                    }
                    if (type instanceof Converter) {
                        term.setValue(((Converter<?>) type).convert(term.getValue()));
                    }
                });
            //适配时间戳字段,查询统一使用timestamp
            if ("timestamp".equals(term.getColumn())) {
                term.setColumn(repository.getTimestampColumn());
            }

            term.setColumn(repository.convertColumnName(term.getColumn()));

            term.setTerms(prepareTerms(table, term.getTerms()));
        }

        return terms;
    }

    protected String buildWhere(String table, QueryParamEntity param, String... and) {
        StringJoiner joiner = new StringJoiner(" ", "where ", "");

        String sql = TDengineQueryConditionBuilder.build(prepareTerms(table, param.getTerms()));

        if (StringUtils.hasText(sql)) {
            joiner.add(sql);
        }

        if (StringUtils.hasText(sql) && and.length > 0) {
            joiner.add("and");
        }
        for (int i = 0; i < and.length; i++) {
            if (i > 0) {
                joiner.add("and");
            }
            joiner.add(and[i]);
        }

        return joiner.length() == 6 ? "" : joiner.toString();
    }


    protected String prepareQueryTable(String metric, QueryParamEntity param) {
        String table = null;
        for (Term term : param.getTerms()) {
            if ("deviceId".equals(term.getColumn())) {
                try {
                    table = getChildTable(metric, String.valueOf(term.getValue()));
                } finally {
                    term.setValue(null);
                }
            }
        }
        if (table != null) {
            return table;
        }
        return repository.getTableForQuery(metric);
    }


    @Override
    protected <T> Flux<T> doQuery(String metric,
                                  QueryParamEntity paramEntity,
                                  Function<TimeSeriesData, T> mapper) {

        StringJoiner joiner = new StringJoiner("");
        joiner.add("select * from")
              .add(" ")
              .add(prepareQueryTable(metric, paramEntity))
              .add(" ")
              .add(buildWhere(metric, paramEntity))
              .add(buildOrderBy(paramEntity));

        if (paramEntity.isPaging()) {
            joiner.add(" limit ").add(String.valueOf(paramEntity.getPageSize()))
                  .add(" offset ")
                  .add(String.valueOf(paramEntity.getPageSize() * paramEntity.getPageIndex()));
        }

        return repository
            .query(joiner.toString())
            .map(mapper);
    }

    @Override
    protected <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                    QueryParamEntity paramEntity,
                                                    Function<TimeSeriesData, T> mapper) {
        return doQueryPager(metric, paramEntity, mapper, t -> true);
    }

    protected <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                    QueryParamEntity query,
                                                    Function<TimeSeriesData, T> mapper,
                                                    Predicate<TimeSeriesData> filter) {
        String sql = "" + prepareQueryTable(metric, query) + " " + buildWhere(metric, query);
        String countSql = "select count(" + repository.convertColumnName("id") + ") total from " + sql;
        String dataSql = "select * from " + sql + buildOrderBy(query)
            + " limit " + query.getPageSize() + " offset " + query.getPageIndex() * query.getPageSize();

        return Mono.zip(
            repository
                .query(countSql)
                .singleOrEmpty()
                .map(data -> data.getInt("total", 0))
                .defaultIfEmpty(0),
            repository
                .query(dataSql)
                .filter(filter)
                .map(mapper)
                .collectList(),
            (total, data) -> PagerResult.of(total, data, query)
        );
    }

    private final Map<String, Map<String, PropertyMetadata>> metricMetadataCache = new ConcurrentHashMap<>();

    private Optional<PropertyMetadata> getMetricColumn(String metric, String column) {
        return Optional.ofNullable(metricMetadataCache.get(metric))
                       .map(cache -> cache.get(column));
    }


    @Nonnull
    @Override
    public Mono<Void> registerMetadata(@Nonnull String productId,
                                       @Nonnull DeviceMetadata metadata) {
        return doReloadMetadata(productId, metadata, true);
    }

    @Nonnull
    @Override
    public Mono<Void> reloadMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata) {
        return doReloadMetadata(productId, metadata, false);
    }

    protected List<TimeSeriesMetadata> createTimeSeriesMetadata(String productId, DeviceMetadata metadata) {
        List<TimeSeriesMetadata> tsMetadata = new ArrayList<>(16);

        for (EventMetadata event : metadata.getEvents()) {
            tsMetadata.add(DeviceTimeSeriesMetadata.event(productId, event));
        }
        tsMetadata.add(DeviceTimeSeriesMetadata.properties(productId, metadata.getProperties()));
        tsMetadata.add(DeviceTimeSeriesMetadata.log(productId));
        return tsMetadata;
    }

    private Mono<Void> doReloadMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata, boolean ddl) {
        Set<String> tags = Collections.singleton("deviceId");
        for (TimeSeriesMetadata timeSeriesMetadata : createTimeSeriesMetadata(productId, metadata)) {
            metricMetadataCache
                .put(timeSeriesMetadata.getMetric().getId(), timeSeriesMetadata
                    .getProperties()
                    .stream()
                    .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity())));
        }
        return Flux
            .concat(Flux
                        .fromIterable(metadata.getEvents())
                        .flatMap(event -> repository
                            .registerMetadata(wrapMetadata(DeviceTimeSeriesMetadata.event(productId, event)), tags, ddl)),
                    repository
                        .registerMetadata(wrapMetadata(DeviceTimeSeriesMetadata.properties(productId, metadata.getProperties())), tags, ddl),
                    repository
                        .registerMetadata(wrapMetadata(DeviceTimeSeriesMetadata.log(productId)), tags, ddl))
            .then();
    }

    private TimeSeriesMetadata wrapMetadata(TimeSeriesMetadata metadata) {
        return metadata;
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachOneProperties(@Nonnull String deviceId,
                                                       @Nonnull QueryParamEntity query,
                                                       @Nonnull String... properties) {

        if (properties.length == 0) {
            return queryEachProperties(deviceId, query.doPaging(0, 1));
        }
        Set<String> propertiesSet = new HashSet<>(Arrays.asList(properties));

        return this
            .queryEachProperties(deviceId, query.doPaging(0, 1))
            .filter(property -> propertiesSet.contains(property.getProperty()));
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
            .filter(prop -> includes.size() > 0 && includes.contains(prop.getId()))
            .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));

        return this
            .doQuery(getPropertyTimeSeriesMetric(productAndMetadata.getT1().getId()), query, Function.identity())
            .flatMap(data -> rowToProperty(data, propertiesMap.values()));
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                                @Nonnull DeviceDataService.AggregationRequest request,
                                                                @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        return aggregationPropertiesByDevice(getProductAndMetadataByProduct(productId), request, properties);
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                               @Nonnull DeviceDataService.AggregationRequest request,
                                                               @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        return aggregationPropertiesByDevice(getProductAndMetadataByDevice(deviceId), request, properties);
    }

    protected String getAggFunction(Aggregation agg) {
        switch (agg) {
            case NONE:
            case FIRST:
                throw new UnsupportedOperationException("不支持的聚合条件:" + agg);
            default:
                return agg.name().toLowerCase();
        }
    }

    protected String getIntervalExpr(Interval interval) {

        return interval.toString();
    }

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
                      .add("(").add(property.getProperty()).add(")")
                      .add(" as ").add(property.getAlias()).add("");
            }
            String table = repository.getTableForQuery(getPropertyTimeSeriesMetric(tp2.getT1().getId()));
            joiner
                .add(" from ")
                .add(table)
                .add(" ")
                .add(buildWhere(table,
                                request.getFilter(),
                                repository.getTimestampColumn() + ">='" + DateFormatter.toString(request.getFrom(), "yyyy-MM-dd HH:mm:ss") + "'",
                                repository.getTimestampColumn() + "<='" + DateFormatter.toString(request.getTo(), "yyyy-MM-dd HH:mm:ss") + "'")
                );
            if (request.getInterval() != null) {
                joiner.add(" interval(").add(getIntervalExpr(request.getInterval())).add(")");
            }
            joiner.add(buildOrderBy(request.getFilter()));

            String format = request.getFormat();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

            return repository
                .query(joiner.toString())
                .map(timeSeriesData -> {
                    long ts = timeSeriesData.getTimestamp();
                    Map<String, Object> newData = timeSeriesData.getData();
                    for (DeviceDataService.DevicePropertyAggregation property : properties) {
                        newData.putIfAbsent(property.getAlias(), 0);
                    }
                    newData.put("time", formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())));
                    return AggregationData.of(newData);
                });

        });

    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                               @Nonnull String property,
                                                               @Nonnull QueryParamEntity query) {
        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMap(productAndMetadata -> this.queryPropertyPage(productAndMetadata, property, query.and("deviceId", "eq", deviceId)));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPageByProductId(@Nonnull String productId,
                                                                          @Nonnull String property,
                                                                          @Nonnull QueryParamEntity query) {
        return this
            .getProductAndMetadataByProduct(productId)
            .flatMap(productAndMetadata -> this.queryPropertyPage(productAndMetadata, property, query));
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
