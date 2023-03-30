package org.jetlinks.pro.external;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.reactivestreams.Publisher;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SpelFunctionFeature implements ValueMapFeature {

    static final String ID = FeatureId.ValueMap.of("spel").getId();
    static ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public Function<ReactorQLRecord, ? extends Publisher<?>> createMapper(Expression expression, ReactorQLMetadata metadata) {
        net.sf.jsqlparser.expression.Function function = ((net.sf.jsqlparser.expression.Function) expression);
        List<Expression> parameters;
        if (function.getParameters() == null
            || CollectionUtils.isEmpty(parameters = function.getParameters().getExpressions())
            || parameters.size() != 1) {
            throw new IllegalArgumentException("函数[spel]必须要有1个参数");
        }

        Expression expr = parameters.get(0);
        if (!(expr instanceof StringValue)) {
            throw new IllegalArgumentException("函数[spel]参数必须为表达式字符");
        }
        org.springframework.expression.Expression spel = expressionParser.parseExpression(((StringValue) expr).getValue());

        return record -> {
            record=record.copy().putRecordToResult();
            Map<String, Object> records = new HashMap<>(record.getRecords(false));
            records.putAll(record.asMap());
            StandardEvaluationContext context=new StandardEvaluationContext();
            context.setVariables(records);
            try {
                return Mono.justOrEmpty(spel.getValue(context));
            } catch (Throwable e) {
                return Mono.error(e);
            }
        };
    }

    @Override
    public String getId() {
        return ID;
    }
}
