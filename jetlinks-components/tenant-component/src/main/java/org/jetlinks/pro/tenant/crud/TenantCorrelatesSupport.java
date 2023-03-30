package org.jetlinks.pro.tenant.crud;

import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.pro.tenant.AssetPermission;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.TenantMember;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * 关联资产操作支持
 *
 * @param <E>
 * @author zhouhao
 * @since 1.2
 */
public interface TenantCorrelatesSupport<E> {
    /**
     * @return 资产类型
     */
    @Authorize(ignore = true)
    @Nonnull
    AssetType getAssetType();

    /**
     * <pre>
     *     public Function&lt;Entity, ?&gt; getAssetIdMapper() {
     *         return Entity::getDeviceId;
     *     }
     * </pre>
     *
     * @return 资产ID转换器
     */
    @Authorize(ignore = true)
    @Nonnull
    Function<E, ?> getAssetIdMapper();

    /**
     * 关联资产的属性，例如: deviceId
     *
     * @return 资产属性
     */
    @Authorize(ignore = true)
    @Nonnull
    String getAssetProperty();

    /**
     * 断言当前登录的用户有对应资产操作权限,如果没有权限则抛出{@link org.hswebframework.web.authorization.exception.AccessDenyException}
     *
     * @param source     数据源
     * @param permission 对数据对操作权限列表
     * @return 处理后对数据
     * @see org.hswebframework.web.authorization.exception.AccessDenyException
     */
    @Authorize(ignore = true)
    default Mono<E> assertPermission(Mono<E> source,
                                     AssetPermission... permission) {

        return TenantMember.assertPermission(source, getAssetType(), getAssetIdMapper(), permission);
    }

    /**
     * 断言当前登录的用户有对应资产操作权限,如果没有权限则抛出{@link org.hswebframework.web.authorization.exception.AccessDenyException}
     *
     * @param source     数据源
     * @param permission 对数据对操作权限列表
     * @return 处理后对数据
     * @see org.hswebframework.web.authorization.exception.AccessDenyException
     */
    @Authorize(ignore = true)
    default Flux<E> assertPermission(Flux<E> source,
                                     AssetPermission... permission) {
        return TenantMember.assertPermission(source, getAssetType(), getAssetIdMapper(), permission);
    }


}
