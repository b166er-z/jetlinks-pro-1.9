package org.jetlinks.pro.notify.manager.service;

import org.jetlinks.pro.notify.NotifierProperties;
import org.jetlinks.pro.notify.NotifyConfigManager;
import org.jetlinks.pro.notify.NotifyType;
import org.jetlinks.pro.notify.manager.entity.NotifyConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Service
public class DefaultNotifyConfigManager implements NotifyConfigManager {

    @Autowired
    private NotifyConfigService configService;

    @Nonnull
    @Override
    public Mono<NotifierProperties> getNotifyConfig(@Nonnull NotifyType notifyType, @Nonnull String configId) {
        return configService.findById(configId)
                .map(NotifyConfigEntity::toProperties);
    }
}
