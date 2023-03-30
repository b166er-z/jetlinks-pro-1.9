package org.jetlinks.pro.notify.template;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.pro.notify.NotifyType;
import org.jetlinks.pro.notify.Provider;
import reactor.core.publisher.Mono;

public interface TemplateProvider {

    NotifyType getType();

    Provider getProvider();

    Mono<? extends Template> createTemplate(TemplateProperties properties);

    default ConfigMetadata getTemplateConfigMetadata() {
        return null;
    }
}
