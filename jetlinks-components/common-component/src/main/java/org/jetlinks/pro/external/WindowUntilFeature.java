package org.jetlinks.pro.external;

import net.sf.jsqlparser.expression.Expression;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.FilterFeature;
import org.jetlinks.reactor.ql.feature.GroupFeature;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class WindowUntilFeature implements GroupFeature {

    private static final String ID = FeatureId.GroupBy.of("_window_until").getId();

    @Override
    public Function<Flux<ReactorQLRecord>, Flux<? extends Flux<ReactorQLRecord>>> createGroupMapper(Expression expression, ReactorQLMetadata metadata) {

        net.sf.jsqlparser.expression.Function func = ((net.sf.jsqlparser.expression.Function) expression);

        List<Expression> parameters;
        if (func.getParameters() == null || CollectionUtils.isEmpty((parameters = func
            .getParameters()
            .getExpressions()))) {
            throw new IllegalArgumentException("函数参数错误:" + func);
        }

        BiFunction<ReactorQLRecord, Object, Mono<Boolean>> mapper = FilterFeature.createPredicateNow(parameters.get(0), metadata);
        return stream -> stream
            .flatMap(record -> Mono.zip(Mono.just(record), Mono.from(mapper.apply(record,record))))
            .windowUntil(Tuple2::getT2,false,Integer.MAX_VALUE)
            .map(window -> window.map(Tuple2::getT1));
    }

    @Override
    public String getId() {
        return ID;
    }
}
