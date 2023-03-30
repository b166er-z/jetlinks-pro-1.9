package org.jetlinks.pro.rule.engine.cluster;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.rule.engine.cluster.SchedulerRegistry;
import org.jetlinks.rule.engine.cluster.TaskSnapshotRepository;
import org.jetlinks.rule.engine.cluster.balancer.DefaultSchedulerLoadBalancer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * 集群调度负载均衡器，用于对调度器任务进行负载均衡处理
 *
 * @author zhouhao
 * @since 1.3
 */
@Component
@ConditionalOnBean(TaskSnapshotRepository.class)
public class ClusterSchedulerLoadBalancer extends DefaultSchedulerLoadBalancer implements CommandLineRunner {
    public ClusterSchedulerLoadBalancer(EventBus eventBus,
                                        SchedulerRegistry registry,
                                        TaskSnapshotRepository snapshotRepository) {
        super(eventBus, registry, snapshotRepository);
    }

    @Override
    @PreDestroy
    public void cleanup() {
        super.cleanup();
    }

    @Override
    public void run(String... args) {
        setup();
    }
}
