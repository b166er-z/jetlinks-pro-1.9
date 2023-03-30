package org.jetlinks.pro.rule.engine.executor;

import com.alibaba.fastjson.JSONObject;
import com.api.jsonata4java.Expression;
import com.api.jsonata4java.expressions.ParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.pro.utils.TimeUtils;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
@Component
@EditorResource(
    id = "delay",
    name = "延迟",
    editor = "rule-engine/editor/common/7-delay.html",
    helper = "rule-engine/i18n/zh-CN/common/89-delay.html",
    order = 20
)
public class DelayTaskExecutorProvider implements TaskExecutorProvider, NodeConverter {

    private final Scheduler scheduler;
    static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getExecutor() {
        return "delay";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DelayTaskExecutor(context, scheduler));
    }

    @Override
    public String getNodeType() {
        return getExecutor();
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel nodeModel = new RuleNodeModel();

        Map<String, Object> newConf = new HashMap<>(nodeJson);
        newConf.remove("x");
        newConf.remove("y");
        newConf.remove("z");
        newConf.remove("name");
        newConf.remove("id");
        newConf.remove("wires");
        newConf.remove("type");
        nodeModel.setConfiguration(newConf);

        return nodeModel;
    }


    static class DelayTaskExecutor extends AbstractTaskExecutor {

        private DelayTaskExecutorConfig config;

        private final Scheduler scheduler;

        public DelayTaskExecutor(ExecutionContext context, Scheduler scheduler) {
            super(context);
            this.scheduler = scheduler;
            reload();
        }

        @Override
        protected Disposable doStart() {
            if (this.disposable != null) {
                this.disposable.dispose();
            }
            return config
                .create(context.getInput().accept(), context, scheduler)
                .map(context::newRuleData)
                .flatMap(ruleData ->
                             context
                                 .fireEvent(RuleConstants.Event.result, ruleData)
                                 .then(context.getOutput().write(Mono.just(ruleData)))
                )
                .onErrorResume(err -> context.onError(err, null))
                .subscribe();
        }

        @Override
        public void reload() {
            super.reload();
            config = DelayTaskExecutorConfig.of(context.getJob().getConfiguration());
        }

        @Override
        public String getName() {
            return "延迟";
        }

    }

    @Getter
    @Setter
    public static class DelayTaskExecutorConfig {

        //延迟类型
        private PauseType pauseType;

        //延迟
        private int timeout;

        //延迟时间单位
        private ChronoUnit timeoutUnits;

        //速率
        private int rate;

        //速率 单位时间
        private int nbRateUnits;

        //速率 单位
        private ChronoUnit rateUnits;

        //随机延迟从
        private int randomFirst;

        //随机延迟至
        private int randomLast;

        //随机延迟单位
        private ChronoUnit randomUnits;

        //分组表达式
        private String groupExpression;

        //丢弃被限流的消息时触发错误事件
        private boolean errorOnDrop;

        public Flux<RuleData> create(Flux<RuleData> flux, ExecutionContext context, Scheduler scheduler) {
            return pauseType.create(this, flux, context, scheduler);
        }

        public static DelayTaskExecutorConfig of(Map<String, Object> configuration) {
            return FastBeanCopier.copy(configuration, new DelayTaskExecutorConfig());
        }
    }

    public enum PauseType {
        delayv {//上游节点指定固定延迟

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {

                return flux
                    .delayUntil(el -> {
                        Map<String, Object> map = RuleDataHelper.toContextMap(el);
                        if (map.get("delay") == null) {
                            return Mono.never();
                        }
                        Duration duration = TimeUtils.parse(String.valueOf(map.get("delay")));
                        context.getLogger().debug("delay execution {} ", duration);
                        return Mono.delay(duration, scheduler);
                    });
            }

        },
        delay {//固定延迟

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {
                return flux
                    .delayUntil(el -> {
                        Duration duration = Duration.of(config.getTimeout(), config.getTimeoutUnits());
                        context.getLogger().debug("delay execution {} ", duration);
                        return Mono.delay(duration, scheduler);
                    });
            }

        },
        random {//随机延迟

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {

                return flux
                    .delayUntil(el -> {
                        Duration duration = Duration.of(
                            ThreadLocalRandom.current().nextLong(
                                config.getRandomFirst(),
                                config.getRandomLast()),
                            config.getRandomUnits());
                        context.getLogger().debug("delay execution {} ", duration);
                        return Mono.delay(duration, scheduler);
                    });
            }
        },
        rate {//速率限制

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {

                Duration duration = Duration.of(config.nbRateUnits, config.getRateUnits());
                return flux
                    .window(duration, scheduler)
                    .flatMap(window -> {
                        AtomicLong counter = new AtomicLong();
                        Flux<RuleData> stream;
                        if (config.isErrorOnDrop()) {//丢弃时触发错误
                            stream = window
                                .index()
                                .flatMap(tp2 -> {
                                    if (tp2.getT1() <= config.getRate()) {
                                        return Mono.just(tp2.getT2());
                                    }
                                    return context.fireEvent(RuleConstants.Event.error, context.newRuleData(tp2.getT2()));
                                });
                        } else {
                            stream = window.take(config.getRate());
                        }
                        return stream
                            .doOnNext(v -> counter.incrementAndGet())
                            .doOnComplete(() -> {
                                if (counter.get() > 0) {
                                    context.getLogger().debug("rate limit execution {}/{}", counter, duration);
                                }
                            })
                            ;
                    }, Integer.MAX_VALUE);
            }
        },
        group {//分组速率限制

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {
                try {
                    Expression expression = Expression.jsonata(config.groupExpression);
                    Function<RuleData, String> mapper = ctx -> {
                        try {
                            return expression
                                .evaluate(objectMapper.valueToTree(RuleDataHelper.toContextMap(ctx)))
                                .asText("null");
                        } catch (Throwable e) {
                            context.onError(e, ctx).subscribe();
                            return "null";
                        }
                    };
                    return flux
                        .groupBy(mapper, Integer.MAX_VALUE)
                        .flatMap(group -> {
                            context.getLogger().debug("start rate limit group [{}]", group.key());
                            return rate.create(config, group, context, scheduler);
                        });
                } catch (ParseException | IOException e) {
                    throw new IllegalArgumentException("分组表达式[" + config.groupExpression + "]格式错误", e);
                }
            }
        };

        abstract Flux<RuleData> create(DelayTaskExecutorConfig config,
                                       Flux<RuleData> flux,
                                       ExecutionContext context,
                                       Scheduler scheduler);

    }
}
