package org.jetlinks.pro.openapi.annotation;


import org.hswebframework.web.authorization.annotation.Dimension;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Dimension(type = "open-api")
public @interface OpenApi {

}
