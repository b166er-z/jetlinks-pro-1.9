package org.jetlinks.pro.simulator.core;

/**
 * 模拟器提供商，用于根据不同的类型来创建对应的模拟器
 *
 * @author zhouhao
 * @see org.jetlinks.pro.simulator.mqtt.MqttSimulatorProvider
 * @see org.jetlinks.pro.simulator.tcp.TcpSimulatorProvider
 * @since 1.6
 */
public interface SimulatorProvider {

    /**
     * @return 模拟器类型
     */
    String getType();

    /**
     * 使用配置创建模拟器
     *
     * @param config 模拟器配置
     * @return 模拟器
     */
    Simulator createSimulator(SimulatorConfig config);

}
