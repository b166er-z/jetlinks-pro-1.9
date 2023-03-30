package org.jetlinks.pro.rule.engine.cluster;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.rule.engine.cluster.strategies.AllSchedulerSelectorStrategy;
import org.jetlinks.pro.rule.engine.cluster.strategies.MinimumLoadSchedulerSelectorStrategy;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.scheduler.Scheduler;
import org.jetlinks.rule.engine.api.scheduler.SchedulerSelector;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群调度器选择器,根据策略选择对应的调度器来执行对应的规则。可通过实现接口{@link SchedulerSelectorStrategy}并注入到Spring
 * 来实现自定义的选择策略
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
public class ClusterSchedulerSelector implements SchedulerSelector, BeanPostProcessor {

    private static final SchedulerSelectorStrategy defaultStrategy = new AllSchedulerSelectorStrategy();

    private final Map<String, SchedulerSelectorStrategy> strategies = new ConcurrentHashMap<>();

    public void addStrategy(SchedulerSelectorStrategy strategy) {
        strategies.put(strategy.getType(), strategy);
    }

    @Override
    public Flux<Scheduler> select(Flux<Scheduler> schedulers, ScheduleJob job) {
        //没指定调度规则,使用默认
        if (job.getSchedulingRule() == null) {
            return defaultStrategy
                .select(schedulers, job.getSchedulingRule())
                .doOnNext(scheduler -> log.debug("select scheduler[{}] for [{}-{}]", scheduler.getId(), job.getInstanceId(), job
                    .getNodeId()));
        }
        return Optional.of(strategies.get(job.getSchedulingRule().getType()))
                       .map(strategy -> strategy.select(schedulers, job.getSchedulingRule()))
                       .orElseGet(() -> defaultStrategy.select(schedulers, job.getSchedulingRule()));
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) {
        if (bean instanceof SchedulerSelectorStrategy) {
            addStrategy(((SchedulerSelectorStrategy) bean));
        }
        return bean;
    }
}
