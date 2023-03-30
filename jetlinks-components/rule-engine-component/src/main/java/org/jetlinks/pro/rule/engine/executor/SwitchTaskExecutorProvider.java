package org.jetlinks.pro.rule.engine.executor;

import com.api.jsonata4java.Expression;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.reactor.ql.utils.CompareUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@EditorResource(
    id = "switch",
    name = "路由",
    editor = "rule-engine/editor/common/10-switch.html",
    helper = "rule-engine/i18n/zh-CN/common/10-switch.html",
    order = 30
)
public class SwitchTaskExecutorProvider implements TaskExecutorProvider {
    @Override
    public String getExecutor() {
        return "switch";
    }

    //条件判断支持
    private final static Map<String, Function<SwitchCondition, BiPredicate<Map<String, Object>, Object>>> supports = new ConcurrentHashMap<>();

    //值类型转换支持
    private final static Map<String, Function<String, Function<Map<String, Object>, Object>>> convertSupports = new ConcurrentHashMap<>();

    static ObjectMapper objectMapper = new ObjectMapper();

    static {

        convertSupports.put("jsonata", expr -> {
            try {
                Expression expression = Expression.jsonata(expr);
                return ctx -> {
                    try {
                        return convertJsonNode(expression.evaluate(objectMapper.valueToTree(ctx)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (Exception e) {
                throw new IllegalArgumentException("jsonata[" + expr + "]格式错误", e);
            }
        });
        convertSupports.put("num", expr -> ctx -> new BigDecimal(expr));
        convertSupports.put("str", expr -> ctx -> expr);
        convertSupports.put("env", expr -> ctx -> System.getProperty(expr));

     /*
        {v: "eq", t: "==", kind: 'V'},
        {v: "neq", t: "!=", kind: 'V'},
        {v: "lt", t: "<", kind: 'V'},
        {v: "lte", t: "<=", kind: 'V'},
        {v: "gt", t: ">", kind: 'V'},
        {v: "gte", t: ">=", kind: 'V'},
        {v: "hask", t: "switch.rules.hask", kind: 'V'},
        {v: "btwn", t: "switch.rules.btwn", kind: 'V'},
        {v: "cont", t: "switch.rules.cont", kind: 'V'},
        {v: "regex", t: "switch.rules.regex", kind: 'V'},
        {v: "true", t: "switch.rules.true", kind: 'V'},
        {v: "false", t: "switch.rules.false", kind: 'V'},
        {v: "null", t: "switch.rules.null", kind: 'V'},
        {v: "nnull", t: "switch.rules.nnull", kind: 'V'},
        {v: "empty", t: "switch.rules.empty", kind: 'V'},
        {v: "nempty", t: "switch.rules.nempty", kind: 'V'},
        {v: "jsonata_exp", t: "switch.rules.exp", kind: 'O'},
        {v: "else", t: "switch.rules.else", kind: 'O'}
     */
        supports.put("eq", createValuePredicate(CompareUtils::equals));
        supports.put("neq", createValuePredicate((left, right) -> !CompareUtils.equals(left, right)));
        supports.put("gt", createValuePredicate((left, right) -> CompareUtils.compare(left, right) > 0));
        supports.put("gte", createValuePredicate((left, right) -> CompareUtils.compare(left, right) >= 0));
        supports.put("lt", createValuePredicate((left, right) -> CompareUtils.compare(left, right) < 0));
        supports.put("lte", createValuePredicate((left, right) -> CompareUtils.compare(left, right) <= 0));
        supports.put("hask", createValuePredicate((left, right) -> left instanceof Map && ((Map<?, ?>) left).containsKey(right)));
        supports.put("cont", createValuePredicate((left, right) -> String.valueOf(left).contains(String.valueOf(right))));
        supports.put("regex", createValuePredicate((left, right) -> String.valueOf(left).matches(String.valueOf(right))));
        supports.put("true", createValuePredicate((left, right) -> CastUtils.castBoolean(left)));
        supports.put("false", createValuePredicate((left, right) -> !CastUtils.castBoolean(left)));
        supports.put("null", createValuePredicate((left, right) -> left == null));
        supports.put("nnull", createValuePredicate((left, right) -> left != null));
        supports.put("empty", createValuePredicate((left, right) -> StringUtils.isEmpty(left)));
        supports.put("nempty", createValuePredicate((left, right) -> !StringUtils.isEmpty(left)));
        supports.put("jsonata_exp", createValuePredicate((left, right) -> CastUtils.castBoolean(right)));
        supports.put("else", createValuePredicate((left, right) -> true));

    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new SwitchTaskExecutor(context));
    }

    public static class SwitchTaskExecutor extends FunctionTaskExecutor {

        private SwitchConfig config;

        public SwitchTaskExecutor(ExecutionContext context) {
            super("路由", context);
            reload();
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            Map<String, Object> ctx = RuleDataHelper.toContextMap(input);
            return Flux.fromIterable(config.match(ctx))
                .flatMapIterable(SwitchCondition::getOutputs)
                .flatMap(nodeId -> context.getOutput().write(nodeId, Mono.just(context.newRuleData(input))))
                .then(Mono.empty());
        }

        @Override
        public void reload() {
            super.reload();
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new SwitchConfig());
            config.prepare();
        }
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
                .map(SwitchTaskExecutorProvider::convertJsonNode)
                .collect(Collectors.toList());
        }
        return jsonNode.asText();
    }

    private static Function<SwitchCondition, BiPredicate<Map<String, Object>, Object>> createValuePredicate(BiPredicate<Object, Object> predicate) {
        return switchCondition -> {
            Function<Map<String, Object>, Object> valueMapper = getValueMapper(switchCondition.valueType, switchCondition.value);

            return (ctx, val) -> predicate.test(val, valueMapper.apply(ctx));
        };
    }

    private static Function<Map<String, Object>, Object> getValueMapper(String type, String expr) {
        if (StringUtils.isEmpty(type)) {
            return ctx -> null;
        }
        return Optional.ofNullable(convertSupports.get(type))
            .orElseThrow(() -> new IllegalArgumentException("不支持的类型:" + type))
            .apply(expr);
    }

    @Getter
    @Setter
    public static class SwitchConfig {
        private String property;
        private String propertyType;
        private List<SwitchCondition> conditions;

        public boolean checkAll;

        @Getter(AccessLevel.PRIVATE)
        @Setter(AccessLevel.PRIVATE)
        private transient Function<Map<String, Object>, Object> valueMapper;

        void prepare() {
            valueMapper = SwitchTaskExecutorProvider.getValueMapper(propertyType, property);

            if (!CollectionUtils.isEmpty(conditions)) {
                conditions.forEach(SwitchCondition::prepare);
            }
        }

        public List<SwitchCondition> match(Map<String, Object> ctx) {
            if (CollectionUtils.isEmpty(conditions)) {
                return Collections.emptyList();
            }
            Object value = valueMapper.apply(ctx);
            List<SwitchCondition> matched = new ArrayList<>();

            for (SwitchCondition rule : conditions) {
                if ("else".equals(rule.getType()) && matched.size() > 0) {
                    continue;
                }
                if (rule.predicate.test(ctx, value)) {
                    matched.add(rule);
                    if (!checkAll) {
                        break;
                    }
                }
            }
            return matched;

        }
    }

    @Getter
    @Setter
    public static class SwitchCondition {
        private String type; //条件类型

        private String value; //参与条件判断的值

        private String valueType; //值类型

        //满足条件后转发数据到指定到节点
        private List<String> outputs = new ArrayList<>();

        @Getter(AccessLevel.PRIVATE)
        @Setter(AccessLevel.PRIVATE)
        private transient BiPredicate<Map<String, Object>, Object> predicate;

        void prepare() {
            predicate =
                Optional.ofNullable(supports.get(type))
                    .orElseThrow(() -> new IllegalArgumentException("不支持的条件类型:" + type))
                    .apply(this);
        }
    }


}
