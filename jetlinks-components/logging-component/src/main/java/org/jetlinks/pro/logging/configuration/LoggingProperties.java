package org.jetlinks.pro.logging.configuration;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.logging.system.SerializableSystemLog;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

@ConfigurationProperties(prefix = "jetlinks.logging")
@Getter
@Setter
public class LoggingProperties {

    /**
     * 系统日志
     *
     * @see lombok.extern.slf4j.Slf4j
     * @see org.slf4j.Logger
     * @see org.jetlinks.pro.logging.logback.SystemLoggingAppender
     * @see SerializableSystemLog
     * @see org.jetlinks.pro.logging.event.SystemLoggingEvent
     */
    @Getter
    @Setter
    private SystemLoggingProperties system = new SystemLoggingProperties();

    /**
     * 访问日志
     *
     * @see org.hswebframework.web.logging.AccessLogger
     * @see org.hswebframework.web.logging.aop.EnableAccessLogger
     * @see org.jetlinks.pro.logging.event.AccessLoggingEvent
     * @see org.jetlinks.pro.logging.access.SerializableAccessLog
     */
    @Setter
    @Getter
    private AccessLoggingProperties access = new AccessLoggingProperties();

    @Getter
    @Setter
    public static class SystemLoggingProperties {
        /**
         * 系统日志上下文,通常用于在日志中标识当前服务等
         *
         * @see org.hswebframework.web.logger.ReactiveLogger#mdc(String, String)
         * @see org.slf4j.MDC
         */
        private Map<String, String> context = new HashMap<>();

    }

    @Getter
    @Setter
    public static class AccessLoggingProperties {
        //指定按path过滤日志
        private List<String> pathExcludes = new ArrayList<>(Collections.singletonList("/authorize/**"));
    }

}
