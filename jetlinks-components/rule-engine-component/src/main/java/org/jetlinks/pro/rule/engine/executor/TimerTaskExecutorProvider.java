package org.jetlinks.pro.rule.engine.executor;

import com.alibaba.fastjson.JSONObject;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.AllArgsConstructor;
import org.jetlinks.pro.ValueObject;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.pro.rule.engine.model.nodes.NodeConverter;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
@AllArgsConstructor
@EditorResource(
    id = "timer",
    name = "定时任务",
    editor = "rule-engine/editor/common/5-timer.html",
    helper = "rule-engine/i18n/zh-CN/common/5-timer.html",
    types = {"timer","schedule-rule"},
    order = 1
)
public class TimerTaskExecutorProvider implements TaskExecutorProvider, NodeConverter {

    private final Scheduler scheduler;

    @Override
    public String getExecutor() {
        return "timer";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new TimerTaskExecutor(context));
    }

    @Override
    public String getNodeType() {
        return getExecutor();
    }

    @Override
    public RuleNodeModel convert(JSONObject nodeJson) {
        RuleNodeModel model = new RuleNodeModel();
        Map<String, Object> config = new HashMap<>();
        config.put("cron", nodeJson.getString("cron"));
        model.setConfiguration(config);

        return model;
    }

    class TimerTaskExecutor extends AbstractTaskExecutor {

        Supplier<Duration> nextDelay;

        public TimerTaskExecutor(ExecutionContext context) {
            super(context);
            nextDelay = createNextDelay();
        }

        @Override
        public String getName() {
            return "定时调度";
        }

        @Override
        protected Disposable doStart() {
            return execute();
        }

        private Disposable execute() {
            Duration nextTime = nextDelay.get();
            context.getLogger().debug("trigger timed task after {}", nextTime);
            if (this.disposable != null) {
                this.disposable.dispose();
            }
            return this.disposable =
                Mono.delay(nextTime, scheduler)
                    .flatMap(t -> context.getOutput().write(Mono.just(context.newRuleData(t))))
                    .then(context.fireEvent(RuleConstants.Event.complete, context.newRuleData(System.currentTimeMillis())).thenReturn(1))
                    .onErrorResume(err -> context.onError(err, null).then(Mono.empty()))
                    .doFinally(s -> {
                       if(getState()== Task.State.running){
                           execute();
                       }
                    })
                    .subscribe();
        }

        @Override
        public void reload() {
            nextDelay = createNextDelay();
            if (disposable != null) {
                disposable.dispose();
            }
            doStart();
        }

        @Override
        public void validate() {
            createNextDelay();
        }

        private Supplier<Duration> createNextDelay() {
            ValueObject config = ValueObject.of(context.getJob().getConfiguration());

            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
            Cron cron = config.getString("cron")
                .map(parser::parse)
                .orElseThrow(() -> new IllegalArgumentException("cron配置不存在"));
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            return () -> executionTime.timeToNextExecution(ZonedDateTime.now()).orElse(Duration.ofSeconds(10));

        }

    }

    public static Flux<ZonedDateTime> getLastExecuteTimes(String cronExpression, Date from, long times) {
        return Flux.create(sink -> {
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
            Cron cron = parser.parse(cronExpression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(from.toInstant(), ZoneId.systemDefault());

            for (long i = 0; i < times; i++) {
                dateTime = executionTime.nextExecution(dateTime)
                    .orElse(null);
                if (dateTime != null) {
                    sink.next(dateTime);
                } else {
                    break;
                }
            }
            sink.complete();


        });
    }
}
