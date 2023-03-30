package org.jetlinks.pro.gateway.ql;

import lombok.AllArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.TableFunction;
import org.jetlinks.core.codec.defaults.JsonCodec;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.FromFeature;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.jetlinks.reactor.ql.supports.ExpressionVisitorAdapter;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * <pre>
 *
 *     select topic,message from message.subscribe( '/device/**' )
 *
 * </pre>
 *
 * @since 1.1
 */
@Component
@AllArgsConstructor
public class EventBusSubscribeFeature implements FromFeature {

    private final static String ID = FeatureId.From.of("message.subscribe").getId();

    private final EventBus eventBus;

    @Override
    public Function<ReactorQLContext, Flux<ReactorQLRecord>> createFromMapper(FromItem fromItem, ReactorQLMetadata metadata) {

        TableFunction from = ((TableFunction) fromItem);
        net.sf.jsqlparser.expression.Function function = from.getFunction();

        if (function.getParameters() == null || CollectionUtils.isEmpty(function.getParameters().getExpressions())) {
            throw new IllegalArgumentException("参数格式错误:" + from);
        }

        List<Expression> parameters = new ArrayList<>(function.getParameters().getExpressions());

        //第一个参数可能是指定是否共享集群消息
        Expression first = parameters.get(0);

        AtomicReference<Boolean> isCluster = new AtomicReference<>();

        first.accept(new ExpressionVisitorAdapter() {
            @Override
            public void visit(Column tableColumn) {
                isCluster.set("true".equals(((Column) first).getColumnName()));
            }

            @Override
            public void visit(LongValue longValue) {
                isCluster.set(longValue.getValue() > 0);
            }

            @Override
            public void visit(StringValue stringValue) {
                isCluster.set(CastUtils.castBoolean(stringValue.getValue()));
            }
        });
        if (isCluster.get() != null) {
            parameters.remove(0);
        }else {
            isCluster.set(false);
        }
        List<Function<ReactorQLRecord, ? extends Publisher<?>>> subs = parameters
            .stream()
            .map(expr -> ValueMapFeature.createMapperNow(expr, metadata))
            .collect(Collectors.toList());


        String alias = from.getAlias() != null ? from.getAlias().getName() : null;

        return ctx -> Flux
            .fromIterable(subs)
            .flatMap(mapper -> mapper.apply(ReactorQLRecord
                                                .newRecord(null, ctx.getParameters(), ctx)
                                                .addRecords(ctx.getParameters())))
            .map(String::valueOf)
            .collectList()
            .flatMapMany(topics -> {
                Subscription sub = Subscription.of("reactor.ql",
                                                   topics.toArray(new String[0]),
                                                   isCluster.get()
                                                       ? new Subscription.Feature[]{Subscription.Feature.local, Subscription.Feature.broker}
                                                       : new Subscription.Feature[]{Subscription.Feature.local});
                return eventBus
                    .subscribe(sub)
                    .map(msg -> ReactorQLRecord.newRecord(alias, mapTopicMessage(msg), ctx));
            });
    }

    public Map<String, Object> mapTopicMessage(TopicPayload message) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("topic", message.getTopic());
        msg.put("message", message.decode(Map.class,true));
        return msg;
    }

    @Override
    public String getId() {
        return ID;
    }
}
