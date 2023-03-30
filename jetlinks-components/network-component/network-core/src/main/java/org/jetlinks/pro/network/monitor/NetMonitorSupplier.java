package org.jetlinks.pro.network.monitor;

/**
 * 网络组件监控提供者,用于提供不同的监控实现
 *
 * @author zhohao
 * @since 1.0
 * @see LogNetMonitor
 * @see MicrometerNetMonitor
 */
public interface NetMonitorSupplier {

    /**
     * 获取监控器
     *
     * @param id   ID
     * @param tags 标签
     * @return NetMonitor
     */
    NetMonitor getMonitor(String id, String... tags);

}
