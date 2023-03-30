package org.jetlinks.pro.simulator.core;


/**
 * 模拟器监听器，用于监听模拟器的生命周期，进行一些自定义的操作，如：
 * <pre>
 *     1. 自定义MQTT认证信息
 *     2. 启动后定时上报数据
 *     3. 自动回复消息
 * </pre>
 *
 * @author zhouhao
 * @since 1.6
 * @see org.jetlinks.pro.simulator.core.listener.SimulatorListenerProvider
 */
public interface SimulatorListener extends Comparable<SimulatorListener> {

    /**
     * @return ID
     */
    String getId();

    /**
     * @return 类型标识
     */
    String getType();

    /**
     * 判断是否支持此模拟器
     *
     * @param simulator 模拟器实例
     * @return 是否支持
     */
    boolean supported(Simulator simulator);

    /**
     * 初始化，当监听器被注册到模拟器时被调用
     *
     * @param simulator 模拟器
     */
    void init(Simulator simulator);

    /**
     * 会话与远程建立连接前调用，可以通过操作会话实例来进行一些自定义的操作。
     * 比如自定义MQTT认证信息
     *
     * @param session 会话实例
     */
    void before(Session session);

    /**
     * 会话与远程建立连接后调用
     *
     * @param session 会话实例
     */
    void after(Session session);

    /**
     * 停止监听器
     */
    void shutdown();

    /**
     * @return 监听器顺序
     */
    default int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    default int compareTo(SimulatorListener o) {
        return Integer.compare(this.order(), o.order());
    }
}
