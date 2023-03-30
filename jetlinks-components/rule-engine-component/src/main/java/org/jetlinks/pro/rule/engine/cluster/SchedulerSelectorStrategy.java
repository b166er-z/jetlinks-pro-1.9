package org.jetlinks.pro.rule.engine.cluster;

import org.jetlinks.pro.rule.engine.cluster.strategies.AllSchedulerSelectorStrategy;
import org.jetlinks.pro.rule.engine.cluster.strategies.MinimumLoadSchedulerSelectorStrategy;
import org.jetlinks.rule.engine.api.scheduler.Scheduler;
import org.jetlinks.rule.engine.api.scheduler.SchedulingRule;
import reactor.core.publisher.Flux;

/**
 * 调度器选择策略
 *
 * @author zhouhao
 * @since 1.3
 */
public interface SchedulerSelectorStrategy {

    /**
     * @return 策略类型
     * @see SchedulingRule#getType()
     */
    String getType();

    /**
     * 选择调度器执行任务
     *
     * @param schedulers 全部调度器
     * @param rule       调度规则
     * @return 选择结果
     */
    Flux<Scheduler> select(Flux<Scheduler> schedulers, SchedulingRule rule);

    /**
     * 创建一个最小负载调度规则,此规则下,将选择负载最小的一个调度器来执行任务.
     *
     * @return 调度规则
     */
    static SchedulingRule minimumLoad() {
        return MinimumLoadSchedulerSelectorStrategy.newRule();
    }

    /**
     * 全部调度规则,此规则下,所有调度器都会调度同一个任务.
     *
     * @return 调度规则
     */
    static SchedulingRule all() {
        return AllSchedulerSelectorStrategy.newRule();
    }

    static boolean isMinimumLoad(SchedulingRule rule) {
        return rule != null && rule.getType().equals(MinimumLoadSchedulerSelectorStrategy.type);
    }

    static boolean isAll(SchedulingRule rule) {
        return rule != null && rule.getType().equals(AllSchedulerSelectorStrategy.type);
    }

}
