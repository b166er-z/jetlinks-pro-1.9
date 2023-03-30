package org.jetlinks.pro.device.tenant;

import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 注解后控制租户设备资产相关权限
 * <pre>
 *     &#064;DeviceAsset(assetIdIndex=0)
 *     public Mono&ltVoid&gt save(String deviceId){
 *
 *     }
 * </pre>
 *
 * @author zhouhao
 * @see 1.2
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@TenantAssets(type = "device")
public @interface DeviceAsset {

    /**
     * @see TenantAssets#assetIdIndex()
     */
    @AliasFor(annotation = TenantAssets.class)
    int assetIdIndex() default 0;

    /**
     * @see TenantAssets#assetObjectIndex()
     */
    @AliasFor(annotation = TenantAssets.class)
    int assetObjectIndex() default -1;

    /**
     * @see TenantAssets#property()
     */
    @AliasFor(annotation = TenantAssets.class)
    String property() default "id";

    /**
     * @see TenantAssets#required()
     */
    @AliasFor(annotation = TenantAssets.class)
    boolean required() default false;

    /**
     * @see TenantAssets#autoBind()
     */
    @AliasFor(annotation = TenantAssets.class)
    boolean autoBind() default false;

    /**
     * @see TenantAssets#ignore()
     */
    @AliasFor(annotation = TenantAssets.class)
    boolean ignore() default false;

    /**
     * @see TenantAssets#ignoreQuery()
     */
    @AliasFor(annotation = TenantAssets.class)
    boolean ignoreQuery() default false;

    /**
     * @see TenantAssets#validate()
     */
    @AliasFor(annotation = TenantAssets.class)
    boolean validate() default true;


}
