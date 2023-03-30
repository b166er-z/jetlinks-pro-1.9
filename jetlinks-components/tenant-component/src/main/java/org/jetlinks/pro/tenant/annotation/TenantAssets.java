package org.jetlinks.pro.tenant.annotation;

import java.lang.annotation.*;

/**
 * <pre>
 *     &#064;TenantAssets(type="device",assetIdArgName="deviceId")
 *     public Mono&ltVoid&gt save(String deviceId){
 *
 *     }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface TenantAssets {

    /**
     * 资产类型
     *
     * @return 资产类型标识
     * @see org.jetlinks.pro.tenant.AssetType
     */
    String type() default "";

    /**
     * @return 资产ID参数索引
     */
    int assetIdIndex() default 0;

    /**
     * @return 资产对象参数索引
     */
    int assetObjectIndex() default -1;

    /**
     * 用于在动态查询时,自动追加查询条件: where(property(),"assets",assetTerm);
     *
     * @return 资产ID对应属性
     * @see org.jetlinks.pro.tenant.term.AssetsTerm
     * @see org.jetlinks.pro.tenant.term.TenantMemberAssetsTermBuilder
     */
    String property() default "id";

    /**
     * 要求当前用户必须是租户,当设置为true时,没有分配租户信息的用户将无法访问此功能
     *
     * @return 默认为false
     */
    boolean required() default false;

    /**
     * 是否自动绑定资产, 通用用于新增数据的时候将新增的实体类自动绑定到用户资产下
     *
     * @return 自动绑定
     */
    boolean autoBind() default false;

    /**
     * 是否自动解绑, 通用用于删除数据的时候,将资产解绑
     *
     * @return 自动绑定
     */
    boolean autoUnbind() default false;

    /**
     * 是否执行校验,通常用于新增数据的时候,设置为false则不校验传入的数据.
     *
     * @return 是否执行校验
     */
    boolean validate() default true;

    /**
     * @return 忽略处理租户权限信息
     */
    boolean ignore() default false;

    /**
     * @return 忽略处理动态查询条件
     */
    boolean ignoreQuery() default false;

    /**
     * 适用于 save or update时
     *
     * @return 当资产不存在时是否通过检查
     */
    boolean allowAssetNotExist() default false;
}
