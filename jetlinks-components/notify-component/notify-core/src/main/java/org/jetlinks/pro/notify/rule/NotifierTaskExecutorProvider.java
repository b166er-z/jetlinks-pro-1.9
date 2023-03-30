package org.jetlinks.pro.notify.rule;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Values;
import org.jetlinks.pro.notify.NotifierManager;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.jetlinks.rule.engine.defaults.LambdaTaskExecutorProvider;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@AllArgsConstructor
@EditorResource(
    id = "notify",
    name = "消息通知",
    editor = "rule-engine/editor/function/19-notify.html",
    helper = "rule-engine/i18n/zh-CN/function/19-notify.html",
    order = 100
)
public class NotifierTaskExecutorProvider implements TaskExecutorProvider, NodeConverter {

    private final NotifierManager notifierManager;

    @Override
    public String getExecutor() {
        return "notifier";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        RuleNotifierProperties properties = FastBeanCopier.copy(context.getJob().getConfiguration(), RuleNotifierProperties.class);
        properties.validate();

        Function<RuleData, Publisher<RuleData>> executor = createExecutor(context, properties);
        return Mono.just(new FunctionTaskExecutor("消息通知", context) {
            @Override
            protected Publisher<RuleData> apply(RuleData input) {
                return executor.apply(input);
            }
        });
    }


    public Function<RuleData, Publisher<RuleData>> createExecutor(ExecutionContext context, RuleNotifierProperties config) {
        return rule -> notifierManager
            .getNotifier(config.getNotifyType(), config.getNotifierId())
            .switchIfEmpty(Mono.fromRunnable(() -> {
                context.getLogger().warn("通知器[{}-{}]不存在", config.getNotifyType(), config.getNotifierId());
            }))
            .flatMap(notifier -> notifier.send(config.getTemplateId(), Values.of(RuleDataHelper.toContextMap(rule))))
            .doOnError(err -> {
                context.getLogger().error("发送[{}]通知[{}-{}]失败",
                    config.getNotifyType().getName(),
                    config.getNotifierId(),
                    config.getTemplateId(), err);
            })
            .doOnSuccess(ignore -> {
                context.getLogger().info("发送[{}]通知[{}-{}]完成",
                    config.getNotifyType().getName(),
                    config.getNotifierId(),
                    config.getTemplateId());
            }).then(Mono.empty());
    }

    @Override
    public String getNodeType() {
        return "notify";
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();

        Map<String, Object> config = new HashMap<>();
        config.put("notifyType", nodeJson.getString("notifyType"));
        config.put("notifierId", nodeJson.getString("notifierId"));
        config.put("templateId", nodeJson.getString("templateId"));

        model.setConfiguration(config);
        model.setExecutor(getExecutor());
        return model;
    }
}
