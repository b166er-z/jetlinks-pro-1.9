package org.jetlinks.pro.gateway.supports;

import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.network.NetworkType;
import reactor.core.publisher.Mono;

/**
 * 设备网关支持提供商,用于提供对各种设备网关的支持.在启动设备网关时,会根据对应的提供商以及配置来创建设备网关.
 * 实现统一管理网关配置,动态创建设备网关.
 *
 * @author zhouhao
 * @see DeviceGateway
 * @since 1.0
 */
public interface DeviceGatewayProvider {

    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 名称
     */
    String getName();

    /**
     * @return 网络类型
     */
    NetworkType getNetworkType();

    /**
     * 使用配置信息创建设备网关
     *
     * @param properties 配置
     * @return void
     */
    Mono<? extends DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties);

}
