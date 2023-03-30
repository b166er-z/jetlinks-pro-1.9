package org.jetlinks.pro.device.features;

import lombok.AllArgsConstructor;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.jetlinks.pro.utils.ReactorQLConditionConverter;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.FromFeature;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.reactor.ql.utils.ExpressionUtils;
import org.jetlinks.reactor.ql.utils.SqlUtils;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *     select max(temp3) from device('demo1');
 *
 *     select temp3 from product('test');
 * </pre>
 */
@AllArgsConstructor
@Component
public class DeviceHistoryPropertiesFromFeature implements FromFeature {

    private final DeviceDataService deviceDataService;

    private static final String ID = FeatureId.From.of("device.properties.history").getId();

    @Override
    public Function<ReactorQLContext, Flux<ReactorQLRecord>> createFromMapper(FromItem fromItem, ReactorQLMetadata metadata) {

        TableFunction from = ((TableFunction) fromItem);

        net.sf.jsqlparser.expression.Function function = from.getFunction();
        ExpressionList list = function.getParameters();
        if (list == null || CollectionUtils.isEmpty(list.getExpressions())) {
            throw new IllegalArgumentException("函数参数错误:" + from);
        }

        List<Expression> parameters = list.getExpressions();
        if (parameters.size() == 1) {
            Expression expr = parameters.get(0);
            SelectBody body = null;
            if (expr instanceof SubSelect) {
                body = ((SubSelect) expr).getSelectBody();
            }
            if (expr instanceof Select) {
                Select select = ((Select) expr);
                body = select.getSelectBody();
            }
            if (body instanceof PlainSelect) {
                return doCreate((PlainSelect) body, metadata);
            }
        }

        throw new IllegalArgumentException("不支持的查询:" + from);
    }

    private Function3<ReactorQLContext, String, QueryParamEntity, Flux<?>> createHistoryPropertiesQuery(List<SelectExpressionItem> select) {

        Map<String, String> aliasMapping = new HashMap<>();

        for (SelectExpressionItem selectExpressionItem : select) {
            String column = getColumnByExpression(selectExpressionItem.getExpression());
            String alias = selectExpressionItem.getAlias() == null ? column : selectExpressionItem.getAlias().getName();
            aliasMapping.put(column, alias);
        }

        String[] properties = aliasMapping.keySet().toArray(new String[0]);

        return (context, deviceId, filter) -> deviceDataService
            .queryProperty(deviceId, filter, properties)
            .map(property -> FastBeanCopier.copy(property, new HashMap<>()));
    }

    private Function3<
        ReactorQLContext,
        QueryParamEntity,
        BiFunction<DeviceDataService.AggregationRequest, DeviceDataService.DevicePropertyAggregation[], Flux<AggregationData>>,
        Flux<?>> createHistoryAggQuery(List<SelectExpressionItem> select,
                                       GroupByElement groupByElement,
                                       int limit) {
        List<DeviceDataService.DevicePropertyAggregation> aggregations = new ArrayList<>(select.size());

        for (SelectExpressionItem selectExpressionItem : select) {
            net.sf.jsqlparser.expression.Function function = (net.sf.jsqlparser.expression.Function) selectExpressionItem.getExpression();
            if (function.getParameters() == null || CollectionUtils.isEmpty(function.getParameters().getExpressions())) {
                throw new IllegalArgumentException("函数未指定列:" + function);
            }
            try {
                Aggregation agg = Aggregation.valueOf(function.getName().toUpperCase());
                Alias alias = selectExpressionItem.getAlias();
                DeviceDataService.DevicePropertyAggregation aggregation = new DeviceDataService.DevicePropertyAggregation();
                aggregation.setAgg(agg);
                aggregation.setProperty(getColumnByExpression(function.getParameters().getExpressions().get(0)));
                if (alias != null) {
                    aggregation.setAlias(alias.getName());
                }
                aggregations.add(aggregation);
            } catch (Throwable e) {
                throw new IllegalArgumentException("不支持的聚合函数:" + function);
            }
        }
        DeviceDataService.DevicePropertyAggregation[] aggs = aggregations.toArray(new DeviceDataService.DevicePropertyAggregation[0]);

        //从分组函数中获取间隔函数
        Interval interval = groupByElement == null ? null : groupByElement.getGroupByExpressions().stream()
            .filter(net.sf.jsqlparser.expression.Function.class::isInstance)
            .map(net.sf.jsqlparser.expression.Function.class::cast)
            .filter(function ->
                "interval".equals(function.getName())
                    && function.getParameters() != null
                    && !CollectionUtils.isEmpty(function.getParameters().getExpressions()))
            .map(function -> ExpressionUtils.getSimpleValue(function.getParameters().getExpressions().get(0)).map(String::valueOf).orElse(null))
            .findFirst()
            .map(Interval::of)
            .orElse(null);

        return (context, filter, executor) -> {

            DeviceDataService.AggregationRequest request = new DeviceDataService.AggregationRequest();
            request.setInterval(interval);
            request.setFormat(interval == null ? "yyyy-MM-dd HH:mm:ss" : interval.getDefaultFormat());
            request.setFilter(filter);
            request.prepareTimestampCondition();
            request.setLimit(limit);

            return executor
                .apply(request, aggs)
                .map(AggregationData::asMap);
        };
    }

    private Function<ReactorQLContext, Flux<String>> createIdSelectorByFunctionParameter(net.sf.jsqlparser.expression.Function function, ReactorQLMetadata metadata) {
        if (function.getParameters() == null || CollectionUtils.isEmpty(function.getParameters().getExpressions())) {
            throw new IllegalArgumentException("函数[" + function + "]参数不能为空");
        }
        List<Function<ReactorQLRecord, ? extends Publisher<?>>> exprs = function
            .getParameters()
            .getExpressions()
            .stream()
            .map(expr -> ValueMapFeature.createMapperNow(expr, metadata))
            .collect(Collectors.toList());

        return ctx -> Flux
            .fromIterable(exprs)
            .flatMap(func -> func.apply(ReactorQLRecord.newRecord(null, ctx.getParameters(), ctx).addRecords(ctx.getParameters())))
            .flatMap(val -> {
                if (val instanceof Map) {
                    if (((Map) val).size() == 0) {
                        return Mono.empty();
                    }
                    return Mono.just(String.valueOf(((Map) val).values().iterator().next()));
                }
                return Mono.just(String.valueOf(val));
            });

    }

    private Executor createExecutor(FromItem item, ReactorQLMetadata metadata) {
        Executor executor = new Executor();
        //默认聚合查询设备
        executor.aggExecutor = deviceDataService::aggregationPropertiesByDevice;
        if (item instanceof TableFunction) {
            //通过函数判断设备ID或者产品ID
            net.sf.jsqlparser.expression.Function func = ((TableFunction) item).getFunction();
            if ("device".equals(func.getName())) {
                executor.idSelector = createIdSelectorByFunctionParameter(func, metadata);
            } else if ("product".equals(func.getName())) {
                executor.product = true;
                executor.idSelector = createIdSelectorByFunctionParameter(func, metadata);
                executor.aggExecutor = deviceDataService::aggregationPropertiesByProduct;
            }
        } else if (item instanceof Table) {
            //设备ID
            String deviceId = SqlUtils.getCleanStr(((Table) item).getName());
            executor.idSelector = ctx -> Flux.just(deviceId);
        }
        if (executor.idSelector == null) {
            Function<ReactorQLContext, Flux<ReactorQLRecord>> fromMapper = FromFeature.createFromMapperByFrom(item, metadata);
            executor.idSelector = ctx -> fromMapper
                .apply(ctx)
                .flatMap(record -> Mono
                    .justOrEmpty(record.asMap().get("deviceId"))
                    .map(String::valueOf));
        }
        return executor;
    }

    private static class Executor {
        boolean product;
        Function<ReactorQLContext, Flux<String>> idSelector;
        Function3<String, DeviceDataService.AggregationRequest, DeviceDataService.DevicePropertyAggregation[], Flux<AggregationData>> aggExecutor;
    }

    private Function<ReactorQLContext, Flux<ReactorQLRecord>> doCreate(PlainSelect select, ReactorQLMetadata metadata) {
        List<SelectItem> items = select.getSelectItems();
        List<SelectExpressionItem> columns = new ArrayList<>();
        AtomicBoolean hasFunction = new AtomicBoolean();

        for (SelectItem item : items) {
            item.accept(new SelectItemVisitor() {
                @Override
                public void visit(AllColumns allColumns) {

                }

                @Override
                public void visit(AllTableColumns allTableColumns) {

                }

                @Override
                public void visit(SelectExpressionItem selectExpressionItem) {
                    Expression expr = selectExpressionItem.getExpression();

                    if (expr instanceof net.sf.jsqlparser.expression.Function) {
                        hasFunction.set(true);
                    } else if (hasFunction.get()) {
                        throw new IllegalArgumentException("函数和属性不能同时查询");
                    }
                    columns.add(selectExpressionItem);

                }
            });
        }

        FromItem fromItem = select.getFromItem();

        String alias = fromItem.getAlias() == null ? null : fromItem.getAlias().getName();

        Executor executor = createExecutor(fromItem, metadata);
        if (!hasFunction.get() && executor.product) {
            throw new IllegalArgumentException("不支持按产品查询设备属性");
        }

        Limit limit = select.getLimit();

        int count = Optional
            .ofNullable(limit)
            .map(Limit::getRowCount)
            .flatMap(ExpressionUtils::getSimpleValue)
            .map(CastUtils::castNumber)
            .map(Number::intValue)
            .orElse(1);

        int offset = Optional
            .ofNullable(limit)
            .map(Limit::getOffset)
            .flatMap(ExpressionUtils::getSimpleValue)
            .map(CastUtils::castNumber)
            .map(Number::intValue)
            .orElse(0);

        ReactorQLConditionConverter conditionConverter = ReactorQLConditionConverter.create(select.getWhere(), metadata);

        //聚合查询
        if (hasFunction.get()) {

            Function3<ReactorQLContext,
                QueryParamEntity,
                BiFunction<DeviceDataService.AggregationRequest,
                    DeviceDataService.DevicePropertyAggregation[],
                    Flux<AggregationData>>, Flux<?>> aggExecutor = createHistoryAggQuery(columns, select.getGroupBy(), count);

            return ctx -> conditionConverter
                //构造查询条件
                .convertQueryParam(ReactorQLRecord.newRecord(alias, ctx.getParameters(), ctx).addRecords(ctx.getParameters()))
                .flatMapMany(query ->
                    //获取ID
                    executor.idSelector.apply(ctx)
                        //每一个ID都单独查询
                        .flatMap(id -> aggExecutor
                            .apply(
                                ctx,
                                query.clone(),
                                (request, devicePropertyAggregations) -> executor.aggExecutor.apply(id, request, devicePropertyAggregations))
                        )
                        .map(data -> ReactorQLRecord.newRecord(alias, data, ctx)));
        } else {
            Function3<ReactorQLContext, String, QueryParamEntity, Flux<?>> datasource = createHistoryPropertiesQuery(columns);

            return ctx ->
                conditionConverter
                    .convertQueryParam(ReactorQLRecord.newRecord(alias, ctx.getParameters(), ctx).addRecords(ctx.getParameters()))
                    .flatMapMany(query -> executor
                        .idSelector
                        .apply(ctx)
                        .flatMap(id -> datasource.apply(ctx, id, query.clone().doPaging(offset / count, count))))
                    .map(data -> ReactorQLRecord.newRecord(alias, data, ctx));
        }
    }

    private String getColumnByExpression(Expression expr) {
        if (expr instanceof Column) {
            return SqlUtils.getCleanStr(((Column) expr).getColumnName());
        } else {
            throw new IllegalArgumentException("不支持的列名表达式:" + expr);
        }
    }

    @Override
    public String getId() {
        return ID;
    }
}
