package org.jetlinks.pro.simulator.core.listener;

import org.jetlinks.pro.simulator.core.SimulatorConfig;
import org.jetlinks.pro.simulator.core.SimulatorListener;
import org.jetlinks.pro.simulator.core.SimulatorListenerBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认监听器构造器
 *
 * @author zhouhao
 * @see JSR223ListenerProvider
 * @see AutoReconnectListenerProvider
 * @since 1.6
 */
@Component
public class DefaultSimulatorListenerBuilder implements SimulatorListenerBuilder {

    private final Map<String, SimulatorListenerProvider> providers = new ConcurrentHashMap<>();

    public DefaultSimulatorListenerBuilder() {
        addProvider(new JSR223ListenerProvider());
        addProvider(new AutoReconnectListenerProvider());
    }

    @Override
    public SimulatorListener build(SimulatorConfig.Listener listener) {
        return Optional.ofNullable(providers.get(listener.getType()))
                       .map(provider -> provider.build(listener))
                       .orElseThrow(() -> new UnsupportedOperationException("unsupported listener type:" + listener.getType()));
    }

    public void addProvider(SimulatorListenerProvider provider) {
        providers.put(provider.getType(), provider);
    }

}
