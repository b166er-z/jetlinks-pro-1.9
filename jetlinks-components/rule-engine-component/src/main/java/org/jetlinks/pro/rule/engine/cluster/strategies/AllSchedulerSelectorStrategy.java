package org.jetlinks.pro.rule.engine.cluster.strategies;

import org.jetlinks.pro.rule.engine.cluster.SchedulerSelectorStrategy;
import org.jetlinks.rule.engine.api.scheduler.Scheduler;
import org.jetlinks.rule.engine.api.scheduler.SchedulingRule;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Comparator;

/**
 * 选择所有调度器来执行规则
 *
 * @author zhouhao
 * @since 1.3
 */
@Component
public class AllSchedulerSelectorStrategy implements SchedulerSelectorStrategy {
    public static final String type = "all";

    public static SchedulingRule newRule() {
        SchedulingRule schedulingRule = new SchedulingRule();

        schedulingRule.setType(type);

        return schedulingRule;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Flux<Scheduler> select(Flux<Scheduler> schedulers, SchedulingRule job) {
        return schedulers;
    }
}
