package org.jetlinks.pro.rule.engine.executor;

import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.LambdaTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DirectTaskExecutorProvider implements TaskExecutorProvider {

    @Override
    public String getExecutor() {
        return "direct";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new LambdaTaskExecutor("direct", context, Mono::just));
    }
}
