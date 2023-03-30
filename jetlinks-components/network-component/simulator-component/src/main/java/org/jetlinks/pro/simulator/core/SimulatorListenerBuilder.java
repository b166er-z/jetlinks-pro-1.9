package org.jetlinks.pro.simulator.core;

/**
 * 监听器构造器，用于根据配置信息来构造监听器
 *
 * @author zhouhao
 * @since 1.6
 */
public interface SimulatorListenerBuilder {

    /**
     * 根据监听器配置信息构造监听器
     *
     * @param listener 监听器配置
     * @return 监听器
     */
    SimulatorListener build(SimulatorConfig.Listener listener);

}
