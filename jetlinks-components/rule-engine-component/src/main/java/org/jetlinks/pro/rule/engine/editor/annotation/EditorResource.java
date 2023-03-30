package org.jetlinks.pro.rule.engine.editor.annotation;


import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface EditorResource {

    String id();

    String name();

    String[] types() default {};

    /**
     * @return 编辑器文件地址
     */
    String editor();

    String helper() default "";

    int order() default -1;
}
