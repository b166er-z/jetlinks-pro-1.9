package org.jetlinks.pro.authorize;

import org.hswebframework.web.authorization.annotation.ResourceAction;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ResourceAction(id ="enable", name = "启用")
public @interface EnableAction {


}
