package org.jetlinks.pro.authorize;

import org.hswebframework.web.authorization.annotation.ResourceAction;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ResourceAction(id ="disable", name = "禁用")
public @interface DisableAction {


}
