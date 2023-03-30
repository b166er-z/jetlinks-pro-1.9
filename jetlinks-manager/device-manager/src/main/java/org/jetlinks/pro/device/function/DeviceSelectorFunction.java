package org.jetlinks.pro.device.function;

import lombok.AllArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.TableFunction;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.service.term.DeviceGroupTerm;
import org.jetlinks.pro.device.service.term.DeviceGroupTreeTerm;
import org.jetlinks.pro.device.service.term.DeviceSameGroupTerm;
import org.jetlinks.pro.device.service.term.DeviceTagTerm;
import org.jetlinks.pro.tenant.term.MultiAssetsTerm;
import org.jetlinks.pro.tenant.term.MultiTenantMemberAssetsTermBuilder;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.FromFeature;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备选择器函数
 *
 * <pre>
 *     select * from device.selector(in_group('tenant1','tenant2'))
 * </pre>
 *
 * <ul>
 *     <li>
 *         in_tenant(tenant1,tenant2): 按租户ID查询
 *     </li>
 *     <li>
 *         in_group(group1,group2): 按分组ID查询
 *     </li>
 *     <li>
 *         in_group_tree(group): 按分组ID以及子分组查询
 *     </li>
 *     <li>
 *         same_group(deviceId): 查询指定设备相同分组的设备
 *     </li>
 *     <li>
 *         tag(key,value):
 *     </li>
 * </ul>
 *
 * @author zhouhao
 * @since 1.4
 */
@Component
@AllArgsConstructor
public class DeviceSelectorFunction implements FromFeature {

    private final static String ID = FeatureId.From.of("device.selector").getId();

    private final ReactiveRepository<DeviceInstanceEntity, String> deviceRepository;

    private static final Map<String, BiConsumer<List<?>, Query<?, QueryParamEntity>>> supports = new ConcurrentHashMap<>();

    static {
        // in_tenant('tenant1','tenant2')
        supports.put("in_tenant", (args, query) ->
            query.accept("id", MultiAssetsTerm.ID, MultiAssetsTerm.from("device", args
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList()))));

        // in_group('group1','group2')
        supports.put("in_group", (args, query) -> query.accept("id", DeviceGroupTerm.termType, args));

        // in_group_tree('group1','group2')
        supports.put("in_group_tree", (args, query) -> query.accept("id", DeviceGroupTreeTerm.termType, args));

        // same_group(deviceId)
        supports.put("same_group", (args, query) -> {
            Object deviceId = args.get(0);
            boolean contains = args.size() > 1 && CastUtils.castBoolean(args.get(0));

            query.and(DeviceSameGroupTerm.createColumn("id", deviceId, contains), deviceId);
        });

        // tag(key,value)
        supports.put("tag", (args, query) -> query.accept("id", DeviceTagTerm.termType, CastUtils.castMap((List<Object>) args)));

        // product('productId')
        supports.put("product", (args, query) -> query.in("productId", args));

        // state('online')
        supports.put("state", (args, query) -> query.in("state", args));

        // device('device1','device2')
        supports.put("device", (args, query) -> query.in("id", args));

        // org('org1')
        supports.put("org", (args, query) -> query.in("orgId", args));

    }

    @Override
    public Function<ReactorQLContext, Flux<ReactorQLRecord>> createFromMapper(FromItem fromItem, ReactorQLMetadata metadata) {

        TableFunction from = ((TableFunction) fromItem);
        net.sf.jsqlparser.expression.Function func = from.getFunction();

        ExpressionList parameters = func.getParameters();
        List<Expression> expressions;
        if (parameters == null || (CollectionUtils.isEmpty(expressions = parameters.getExpressions()))) {
            throw new IllegalArgumentException("device.selector函数参数不能为空.");
        }
        if (!expressions.stream().allMatch(expr -> expr instanceof net.sf.jsqlparser.expression.Function)) {
            throw new IllegalArgumentException("device.selector函数参数只支持函数.");
        }

        List<BiFunction<ReactorQLContext, Query<?, QueryParamEntity>, Mono<Void>>>
            allMapper = expressions.stream()
                                   .map(expr -> createQueryParamMapper(expr, metadata)).collect(Collectors.toList());


        String alias = from.getAlias() != null ? from.getAlias().getName() : null;

        return ctx -> QueryParamEntity
            .newQuery()
            .noPaging()
            .includes(
                DeviceInstanceEntity::getId,
                DeviceInstanceEntity::getName,
                DeviceInstanceEntity::getProductId,
                DeviceInstanceEntity::getProductName,
                DeviceInstanceEntity::getOrgId
            ).as(query -> Flux
                .fromIterable(allMapper)
                .flatMap(mapper -> mapper.apply(ctx, query))
                .then(Mono.just(query)))
            .flatMapMany(query -> query.execute(deviceRepository.createQuery()::setParam).fetch())
            .map(instance -> ReactorQLRecord.newRecord(alias, FastBeanCopier.copy(instance, HashMap::new), ctx));
    }

    private BiFunction<ReactorQLContext, Query<?, QueryParamEntity>, Mono<Void>> createQueryParamMapper(Expression funcExpr, ReactorQLMetadata metadata) {
        net.sf.jsqlparser.expression.Function function = ((net.sf.jsqlparser.expression.Function) funcExpr);

        List<Expression> expressions = function.getParameters() == null || function
            .getParameters()
            .getExpressions() == null
            ? Collections.emptyList()
            : function.getParameters().getExpressions();

        List<Function<ReactorQLRecord, ? extends Publisher<?>>> argMappers = expressions.stream()
                                                                                        .map(expr -> ValueMapFeature.createMapperNow(expr, metadata))
                                                                                        .collect(Collectors.toList());

        BiConsumer<List<?>, Query<?, QueryParamEntity>> builder = supports.get(function.getName().toLowerCase());

        return (ctx, query) -> {
            ReactorQLRecord record = ReactorQLRecord.newRecord(null, null, ctx).addRecords(ctx.getParameters());
            return Flux.fromIterable(argMappers)
                       .flatMap(mapper -> Mono.from(mapper.apply(record)))
                       .collectList()
                       .doOnNext(args -> builder.accept(args, query))
                       .then();
        };

    }

    @Override
    public String getId() {
        return ID;
    }
}
