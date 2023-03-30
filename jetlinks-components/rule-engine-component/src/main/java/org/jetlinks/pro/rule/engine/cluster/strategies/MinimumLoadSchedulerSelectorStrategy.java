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
 * 选择最小负载对调度器来执行规则
 *
 * @author zhouhao
 * @since 1.3
 */
@Component
public class MinimumLoadSchedulerSelectorStrategy implements SchedulerSelectorStrategy {

    public static final String type = "min_load";

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
        return schedulers
            .flatMap(scheduler -> scheduler.totalTask().zipWith(Mono.just(scheduler)))
            .sort(Comparator.comparing(Tuple2::getT1))
            .take(1)
            .map(Tuple2::getT2)
            ;
    }
}
