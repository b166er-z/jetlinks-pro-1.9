package org.jetlinks.pro.external;

import net.sf.jsqlparser.expression.Expression;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.GroupFeature;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.jetlinks.reactor.ql.utils.CompareUtils;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class WindowUntilChangeFeature implements GroupFeature {

    private static final String ID = FeatureId.GroupBy.of("_window_until_change").getId();

    @Override
    public Function<Flux<ReactorQLRecord>, Flux<? extends Flux<ReactorQLRecord>>> createGroupMapper(Expression expression, ReactorQLMetadata metadata) {

        net.sf.jsqlparser.expression.Function func = ((net.sf.jsqlparser.expression.Function) expression);

        List<Expression> parameters;
        if (func.getParameters() == null || CollectionUtils.isEmpty((parameters = func
            .getParameters()
            .getExpressions()))) {
            throw new IllegalArgumentException("函数参数错误:" + func);
        }

        Function<ReactorQLRecord, ? extends Publisher<?>> mapper = ValueMapFeature.createMapperNow(parameters.get(0), metadata);
        return stream -> {
            ChangedPredicate predicate = new ChangedPredicate();

            return stream
                .flatMap(record -> Mono.zip(Mono.just(record), Mono.from(mapper.apply(record))))
                .windowUntil(predicate::test,false,Integer.MAX_VALUE)
                .map(window -> {
                    ReactorQLRecord last = predicate.lastRecord;
                    Map<String, Object> result = last == null ? null : last.putRecordToResult().asMap();
                    return window
                        .map(Tuple2::getT1)
                        .doOnNext(record -> {
                            if (result != null) {
                                record.setResult("_change_before", result);
                            }
                        });
                });
        };
    }

    static class ChangedPredicate implements Predicate<Tuple2<ReactorQLRecord, ?>>, Disposable {


        private Object lastKey;
        private ReactorQLRecord lastRecord;

        ChangedPredicate() {

        }

        @Override
        public void dispose() {
            lastKey = null;
        }

        @Override
        public boolean test(Tuple2<ReactorQLRecord, ?> t) {
            Object k = t.getT2();
            ReactorQLRecord record = t.getT1();
            if (null == lastKey) {
                lastKey = k;
                return false;
            }

            boolean match;
            match = CompareUtils.equals(lastKey, k);
            lastKey = k;
            lastRecord = record;
            return !match;
        }

    }

    @Override
    public String getId() {
        return ID;
    }
}
