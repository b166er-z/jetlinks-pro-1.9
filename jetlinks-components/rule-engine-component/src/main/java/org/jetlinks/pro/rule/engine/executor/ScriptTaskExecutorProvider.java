package org.jetlinks.pro.rule.engine.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.api.scripting.JSObject;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.LambdaTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Slf4j
@EditorResource(
    id = "function",
    name = "函数",
    editor = "rule-engine/editor/function/10-function.html",
    helper = "rule-engine/i18n/zh-CN/function/10-function.html",
    order = 70
)
public class ScriptTaskExecutorProvider implements TaskExecutorProvider, NodeConverter {

    @Override
    public String getExecutor() {
        return "script";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new LambdaTaskExecutor("script", context, () -> createExecutor(context, FastBeanCopier.copy(context.getJob().getConfiguration(), new ScriptConfig()))));
    }

    @SneakyThrows
    public Function<RuleData, Publisher<?>> createExecutor(ExecutionContext context, ScriptConfig config) {

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(config.getLang());
        if (engine == null) {
            throw new UnsupportedOperationException("不支持的脚本语言:" + config.getLang());
        }
        if (StringUtils.isEmpty(config.getScript())) {
            log.warn("script is empty");
            return Mono::just;
        }
        String id = DigestUtils.md5Hex(config.getScript());
        if (!engine.compiled(id)) {
            engine.compile(id, config.getScript());
        }

        Handler handler = new Handler();
        Map<String, Object> scriptContext = new HashMap<>();
        scriptContext.put("context", context);
        scriptContext.put("handler", handler);
        engine.execute(id, scriptContext).getIfSuccess();
        List<List<String>> outputs = config.getOutputs();
        return ruleData -> Flux.defer(() -> {
            if (handler.onMessage != null) {
                Object result = handler.onMessage.apply(ruleData);
                if (isUndefined(result)) {
                    return Flux.empty();
                }
                if (result instanceof Publisher) {
                    return Flux.from(((Publisher<?>) result));
                }
                if (result instanceof Map) {
                    result = new HashMap<>((Map<?, ?>) result);
                }
                return Flux.just(result);
            }
            return Flux.empty();
        }).flatMap(val -> {
            if (val instanceof JSObject) {
                val = convertJSObject(((JSObject) val));
            }
            if (!CollectionUtils.isEmpty(outputs)) {
                Flux<?> flux;
                if (val instanceof Iterable) {
                    flux = Flux
                        .fromStream(StreamSupport.stream(((Iterable<?>) val).spliterator(), false)
                            .map(v -> {
                                if (isUndefined(v)) {
                                    return empty;
                                }
                                return v;
                            })
                        );
                } else if (val instanceof Publisher) {
                    flux = Flux.from(((Publisher<?>) val));
                } else {
                    flux = Flux.just(val);
                }
                return flux
                    .index()
                    .filter(tp2 -> tp2.getT2() != empty) //跳过empty的输出
                    .flatMap(tp2 -> {
                        if (outputs.size() > tp2.getT1()) {
                            RuleData output = context.newRuleData(ruleData.newData(tp2.getT2()));
                            return context
                                .fireEvent(RuleConstants.Event.result, output)
                                .then(Flux
                                    .fromIterable(outputs.get(tp2.getT1().intValue()))
                                    .flatMap(node -> context.getOutput().write(node, Mono.just(output)))
                                    .then());
                        } else {
                            context.getLogger().warn("函数输出数量[{}]与配置的输出数量[{}]不一致", tp2.getT1(), outputs.size());
                        }
                        return Mono.empty();
                    })
                    .then();
            }
            return Mono.just(val);
        });
    }

    static Empty empty = new Empty();

    private static final class Empty {

    }

    private boolean isUndefined(Object value) {
        return value == null || value.getClass().getName().equals("jdk.nashorn.internal.runtime.Undefined");
    }

    private Object convertJSObject(JSObject jsObject) {
        if (jsObject.isArray()) {
            return jsObject
                .values()
                .stream()
                .map(obj -> {
                    if (obj instanceof JSObject) {
                        return convertJSObject(((JSObject) obj));
                    }
                    return obj;
                }).collect(Collectors.toList());

        }
        return JSON.toJSON(jsObject);
    }

    @Override
    public String getNodeType() {
        return "function";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel nodeModel = new RuleNodeModel();
        Map<String, Object> config = new HashMap<>();

        config.put("script", nodeJson.getString("func"));

        if (nodeJson.getIntValue("outputs") > 1) {
            JSONArray wires = nodeJson.getJSONArray("wires");
            config.put("outputs", wires);
            wires.remove("wires");
        }

        nodeModel.setConfiguration(config);
        nodeModel.setExecutor(getExecutor());

        return nodeModel;
    }

    public static class Handler {
        private Function<RuleData, Object> onMessage;

        public void onMessage(Function<RuleData, Object> onMessage) {
            this.onMessage = onMessage;
        }
    }

    @Getter
    @Setter
    public static class ScriptConfig {

        private String lang = "js";

        private String script;

        private List<List<String>> outputs;

    }
}
