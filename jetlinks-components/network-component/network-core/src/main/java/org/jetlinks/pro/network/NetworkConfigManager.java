package org.jetlinks.pro.network;

import reactor.core.publisher.Mono;

/**
 * 网络组件配置管理器
 *
 * @author zhouhao
 * @since 1.0
 */
public interface NetworkConfigManager {

    /**
     * 根据网络类型和配置ID获取配置信息
     *
     * @param networkType 网络类型
     * @param id          配置ID
     * @return 配置信息
     */
    Mono<NetworkProperties> getConfig(NetworkType networkType, String id);

}
