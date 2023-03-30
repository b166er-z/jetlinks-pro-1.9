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
import org.jetlinks.pro.device.service.data.DeviceLatestDataService;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationColumn;
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
public class DeviceLatestPropertiesFromFeature implements FromFeature {

    private final DeviceLatestDataService deviceDataService;

    private static final String ID = FeatureId.From.of("device.properties.latest").getId();

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

        Set<String> columns = new HashSet<>();
        if (!aliasMapping.isEmpty()) {
            columns.add("id");
            columns.add("deviceName");
            columns.addAll(aliasMapping.keySet());
        }

        String[] properties = columns.toArray(new String[0]);

        return (context, productId, filter) ->
            deviceDataService
                .query(productId, filter.includes(properties))
                .map(data -> {
                    if (aliasMapping.isEmpty()) {
                        return data;
                    }
                    Map<String, Object> newData = new HashMap<>();
                    for (Map.Entry<String, String> prop : aliasMapping.entrySet()) {
                        Object value = data.get(prop.getKey());
                        if (value != null) {
                            newData.put(prop.getValue(), value);
                        }
                    }
                    return newData;
                });
    }

    private Function3<ReactorQLContext, String, QueryParamEntity, Flux<?>> createHistoryAggQuery(List<SelectExpressionItem> select) {
        List<AggregationColumn> aggregations = new ArrayList<>(select.size());

        for (SelectExpressionItem selectExpressionItem : select) {
            net.sf.jsqlparser.expression.Function function = (net.sf.jsqlparser.expression.Function) selectExpressionItem.getExpression();
            if (function.getParameters() == null || CollectionUtils.isEmpty(function.getParameters().getExpressions())) {
                throw new IllegalArgumentException("函数未指定列:" + function);
            }
            try {
                Aggregation agg = Aggregation.valueOf(function.getName().toUpperCase());
                Alias alias = selectExpressionItem.getAlias();
                AggregationColumn column = new AggregationColumn();
                column.setProperty(getColumnByExpression(function.getParameters().getExpressions().get(0)));
                column.setAggregation(agg);
                if (alias != null) {
                    column.setAlias(alias.getName());
                }
                aggregations.add(column);
            } catch (Throwable e) {
                throw new IllegalArgumentException("不支持的聚合函数:" + function);
            }
        }
        return (context, id, filter) -> deviceDataService
            .aggregation(id, aggregations, filter)
            .flux();
    }

    private Executor createExecutor(FromItem item, ReactorQLMetadata metadata) {
        Executor executor = new Executor();
        //默认聚合查询设备
        if (item instanceof Table) {
            //产品ID
            String deviceId = SqlUtils.getCleanStr(((Table) item).getName());
            executor.idSelector = ctx -> Flux.just(deviceId);
        }
        if (executor.idSelector == null) {
            Function<ReactorQLContext, Flux<ReactorQLRecord>> fromMapper = FromFeature.createFromMapperByFrom(item, metadata);
            executor.idSelector = ctx -> fromMapper
                .apply(ctx)
                .flatMap(record -> Mono
                    .justOrEmpty(record.asMap().get("deviceId"))
                    .map(String::valueOf)
                );
        }
        return executor;
    }

    private static class Executor {
        boolean product;
        Function<ReactorQLContext, Flux<String>> idSelector;
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

            Function3<ReactorQLContext, String, QueryParamEntity, Flux<?>> aggExecutor = createHistoryAggQuery(columns);

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
                                id,
                                query.clone())
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
