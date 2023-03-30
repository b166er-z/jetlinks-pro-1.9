package org.jetlinks.pro.simulator.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的模拟器管理器实现
 *
 * @author zhouhao
 * @since 1.6
 */
@Component
public class DefaultSimulatorManager implements SimulatorManager, BeanPostProcessor {

    Map<String, Simulator> simulators = new ConcurrentHashMap<>();

    Map<String, SimulatorProvider> providers = new ConcurrentHashMap<>();

    @Override
    public Mono<Simulator> getSimulator(String id) {
        return Mono.justOrEmpty(simulators.get(id));
    }

    @Override
    public Mono<Void> remove(String id) {
        simulators.remove(id);
        return Mono.empty();
    }

    @Override
    public Mono<Simulator> createSimulator(SimulatorConfig config) {

        return Mono
            .justOrEmpty(providers.get(config.getType()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported type:" + config.getType())))
            .map(provider -> provider.createSimulator(config))
            .doOnNext(simulator -> simulators.put(config.getId(), simulator))
            ;
    }

    public void addProvider(SimulatorProvider provider) {
        providers.put(provider.getType(), provider);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SimulatorProvider) {
            addProvider(((SimulatorProvider) bean));
        }
        return bean;
    }
}
