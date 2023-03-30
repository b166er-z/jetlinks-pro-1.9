package org.jetlinks.pro.device.tenant;

import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * <pre>
 *     &#064;DeviceGroupAsset
 *     public Mono&ltVoid&gt save(String deviceId){
 *
 *     }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@TenantAssets(type = "deviceGroup")
public @interface DeviceGroupAsset {

    @AliasFor(annotation = TenantAssets.class)
    int assetIdIndex() default -1;

    @AliasFor(annotation = TenantAssets.class)
    int assetObjectIndex() default -1;

    @AliasFor(annotation = TenantAssets.class)
    String property() default "id";

    @AliasFor(annotation = TenantAssets.class)
    boolean required() default false;

    @AliasFor(annotation = TenantAssets.class)
    boolean autoBind() default false;

    @AliasFor(annotation = TenantAssets.class)
    boolean ignore() default false;

    @AliasFor(annotation = TenantAssets.class)
    boolean validate() default true;

}
