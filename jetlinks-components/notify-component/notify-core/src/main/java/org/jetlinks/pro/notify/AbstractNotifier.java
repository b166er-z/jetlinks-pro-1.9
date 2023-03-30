package org.jetlinks.pro.notify;

import lombok.AllArgsConstructor;
import org.jetlinks.core.Values;
import org.jetlinks.pro.notify.template.Template;
import org.jetlinks.pro.notify.template.TemplateManager;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

@AllArgsConstructor
public abstract class AbstractNotifier<T extends Template> implements Notifier<T> {

    private final TemplateManager templateManager;

    @Override
    @Nonnull
    public Mono<Void> send(@Nonnull String templateId, @Nonnull Values context) {
        return templateManager
                .getTemplate(getType(), templateId)
                .switchIfEmpty(Mono.error(new UnsupportedOperationException("模版不存在:" + templateId)))
                .flatMap(tem -> send((T) tem, context));
    }


}
