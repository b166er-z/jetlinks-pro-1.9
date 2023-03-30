package org.jetlinks.pro.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JSONataFunctionFeature implements ValueMapFeature {

    static final String ID = FeatureId.ValueMap.of("jsonata").getId();
    static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Function<ReactorQLRecord, ? extends Publisher<?>> createMapper(Expression expression, ReactorQLMetadata metadata) {
        net.sf.jsqlparser.expression.Function function = ((net.sf.jsqlparser.expression.Function) expression);
        List<Expression> parameters;
        if (function.getParameters() == null
            || CollectionUtils.isEmpty(parameters = function.getParameters().getExpressions())
            || parameters.size() != 1) {
            throw new IllegalArgumentException("函数[jsonata]必须要有1个参数");
        }

        Expression expr = parameters.get(0);
        if (!(expr instanceof StringValue)) {
            throw new IllegalArgumentException("函数[jsonata]参数必须为表达式字符");
        }
        com.api.jsonata4java.Expression jsonataExpr;
        try {
            jsonataExpr = com.api.jsonata4java.Expression.jsonata(((StringValue) expr).getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("jsonata表达式错误", e);
        }
        return record -> {
            record = record.copy();
            record.putRecordToResult();

            Map<String, Object> records = new HashMap<>(record.getRecords(true));
            records.putAll(record.asMap());
            try {
                return Mono.justOrEmpty(convertJsonNode(jsonataExpr.evaluate(objectMapper.valueToTree(records))));
            } catch (Throwable e) {
                return Mono.error(e);
            }
        };
    }

    @SneakyThrows
    static Object convertJsonNode(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        if (jsonNode.isBoolean()) {
            return jsonNode.booleanValue();
        }
        if (jsonNode.isNumber()) {
            return jsonNode.numberValue();
        }
        if (jsonNode.isNull()) {
            return null;
        }
        if (jsonNode.isObject()) {
            return objectMapper.treeToValue(jsonNode, Map.class);
        }
        if (jsonNode.isArray()) {
            return StreamSupport.stream(jsonNode.spliterator(), false)
                .map(JSONataFunctionFeature::convertJsonNode)
                .collect(Collectors.toList());
        }
        return jsonNode.asText();
    }

    @Override
    public String getId() {
        return ID;
    }
}
