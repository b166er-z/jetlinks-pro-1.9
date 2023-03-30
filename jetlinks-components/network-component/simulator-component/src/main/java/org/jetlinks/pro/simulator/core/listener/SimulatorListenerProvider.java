package org.jetlinks.pro.simulator.core.listener;

import org.jetlinks.pro.simulator.core.SimulatorConfig;
import org.jetlinks.pro.simulator.core.SimulatorListener;

/**
 * 模拟器监听器实现提供者，用于根据监听器类型创建不同的监听器
 *
 * @author zhouhao
 * @since 1.6
 */
public interface SimulatorListenerProvider {

    /**
     * @return 监听器类型
     */
    String getType();

    /**
     * 根据配置创建监听器
     *
     * @param listener 监听器配置
     * @return 监听器
     */
    SimulatorListener build(SimulatorConfig.Listener listener);

}
