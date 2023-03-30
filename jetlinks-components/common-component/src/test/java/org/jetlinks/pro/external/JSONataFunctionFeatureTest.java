package org.jetlinks.pro.external;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JSONataFunctionFeatureTest {


    @Test
    void test() {
        doTest("value",Collections.singletonMap("value",123),123);
        doTest("value+1",Collections.singletonMap("value",123),124L);

        doTest("this.value",Collections.singletonMap("value",123),123);

        doTest("value[0]",Collections.singletonMap("value", Collections.singletonList(123)),123);


    }

    void doTest(String expr, Map<String, Object> val, Object expect) {
        JSONataFunctionFeature feature = new JSONataFunctionFeature();
        Function function = new Function();
        function.setParameters(new ExpressionList(Arrays.asList(new StringValue(expr))));
        Mono.from(feature
            .createMapper(function, new DefaultReactorQLMetadata(new PlainSelect()))
            .apply(ReactorQLRecord.newRecord(null, val, ReactorQLContext.ofDatasource(id -> Mono.empty())))
        ).cast(Object.class)
            .as(StepVerifier::create)
            .expectNext(expect)
            .verifyComplete()

        ;
    }

}