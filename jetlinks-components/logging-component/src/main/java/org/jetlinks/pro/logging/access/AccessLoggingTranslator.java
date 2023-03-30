package org.jetlinks.pro.logging.access;

import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.logging.configuration.LoggingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AccessLoggingTranslator {

    private final ApplicationEventPublisher eventPublisher;

    private final LoggingProperties properties;

    public AccessLoggingTranslator(ApplicationEventPublisher eventPublisher, LoggingProperties properties) {
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @EventListener
    public void translate(AccessLoggerAfterEvent event) {

        for (String pathExclude : properties.getAccess().getPathExcludes()) {
            if(TopicUtils.match(event.getLogger().getUrl(),pathExclude)){
                return;
            }
        }
        eventPublisher.publishEvent(SerializableAccessLog.of(event.getLogger()));
    }

}
