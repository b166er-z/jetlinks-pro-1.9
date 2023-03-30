package org.jetlinks.pro.gateway.annotation;

import org.jetlinks.core.event.Subscription;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 订阅来自消息网关的消息
 *
 * @author zhouhao
 * @since 1.0
 * @see org.jetlinks.core.event.EventBus
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Subscribe {

    @AliasFor("value")
    String[] topics() default {};

    @AliasFor("topics")
    String[] value() default {};

    String id() default "";

    Subscription.Feature[] features() default Subscription.Feature.local;

}
