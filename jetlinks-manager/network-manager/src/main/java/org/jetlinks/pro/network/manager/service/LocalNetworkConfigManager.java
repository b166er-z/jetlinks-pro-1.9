package org.jetlinks.pro.network.manager.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.pro.network.NetworkConfigManager;
import org.jetlinks.pro.network.NetworkProperties;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.manager.entity.NetworkConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LocalNetworkConfigManager implements NetworkConfigManager {

    private final ReactiveRepository<NetworkConfigEntity, String> reactiveRepository;

    private final String serverId;

    public LocalNetworkConfigManager(ReactiveRepository<NetworkConfigEntity, String> reactiveRepository,
                                     ClusterManager clusterManager) {
        this.reactiveRepository = reactiveRepository;
        this.serverId = clusterManager.getCurrentServerId();
    }

    @Override
    public Mono<NetworkProperties> getConfig(NetworkType networkType, String id) {
        return reactiveRepository
            .findById(id)
            .flatMap(conf -> Mono.justOrEmpty(conf.toNetworkProperties(serverId)));
    }
}
