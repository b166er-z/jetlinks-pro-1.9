package org.jetlinks.pro.tenant;

import lombok.NonNull;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.User;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.pro.tenant.supports.CurrentTenantHolder;
import org.jetlinks.pro.tenant.supports.MultiTenantMember;
import org.jetlinks.pro.tenant.term.AssetsTerm;
import org.jetlinks.pro.tenant.term.MultiAssetsTerm;
import org.jetlinks.pro.tenant.term.MultiTenantMemberAssetsTermBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 当前租户成员信息,用于获取当前访问系统的租户成员信息.进行判定资产权限,权限控制等.
 *
 * @author zhouhao
 * @since 1.2
 */
public interface TenantMember {

    /**
     * @return 租户信息
     */
    @NonNull
    Tenant getTenant();

    /**
     * @return 系统用户ID
     * @see Authentication#getUser()
     * @see User#getId()
     */
    @NonNull
    String getUserId();

    /**
     * @return 是否为管理员
     */
    boolean isAdmin();

    /**
     * @return 是否为主租户
     */
    boolean isMain();

    /**
     * 根据ID获取资产信息
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return 资产信息
     */
    @NonNull Mono<TenantAsset> getAsset(@NonNull AssetType assetType,
                                        @NonNull String assetId);

    /**
     * 根据ID获取资产信息
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return 资产信息
     */
    @NonNull Mono<TenantAsset> getAsset(@NonNull String assetType,
                                        @NonNull String assetId);

    /**
     * 获取多个资产信息
     *
     * @param assetType 资产类型
     * @param assetId   资产ID集合
     * @return 资产信息
     */
    @NonNull Flux<TenantAsset> getAssets(@NonNull AssetType assetType,
                                         @NonNull Collection<?> assetId);

    /**
     * 获取多个资产信息
     *
     * @param assetType 资产类型
     * @param assetId   资产ID集合
     * @return 资产信息
     */
    @NonNull Flux<TenantAsset> getAssets(@NonNull String assetType,
                                         @NonNull Collection<?> assetId);

    /**
     * 获取指定类型对全部资产
     *
     * @param assetType 资产类型
     * @return 资产集合
     */
    @NonNull Flux<TenantAsset> getAssets(@NonNull AssetType assetType);

    /**
     * 获取指定类型对全部资产
     *
     * @param assetType 资产类型
     * @return 资产集合
     */
    @NonNull Flux<TenantAsset> getAssets(@NonNull String assetType);


    /**
     * 获取指定ID的资产
     *
     * @param assetType 资产类型
     * @param assetId   资产ID列表
     * @return 资产集合
     */
    default @NonNull Flux<TenantAsset> getAssets(@NonNull AssetType assetType,
                                                 @NonNull String... assetId) {
        return getAssets(assetType, Arrays.asList(assetId));
    }

    /**
     * 绑定资产到当前用户下
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return void
     */
    @Nonnull
    Mono<Void> bindAssets(@NonNull AssetType assetType,
                          @NonNull Collection<?> assetId);

    /**
     * 绑定资产到当前用户下
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return void
     */
    @Nonnull
    Mono<Void> bindAssets(@NonNull String assetType,
                          @NonNull Collection<?> assetId);

    /**
     * 解除绑定资产到当前用户下
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return void
     */
    @Nonnull
    Mono<Void> unbindAssets(@NonNull AssetType assetType,
                            @NonNull Collection<?> assetId);

    /**
     * 解除绑定资产到当前用户下
     *
     * @param assetType 资产类型
     * @param assetId   资产ID
     * @return void
     */
    @Nonnull
    Mono<Void> unbindAssets(@NonNull String assetType,
                            @NonNull Collection<?> assetId);

    /**
     * 判定是否有指定资产的操作权限
     *
     * @param assetType  资产类型
     * @param assetId    资产ID集合
     * @param permission 权限
     * @return 是否有权限
     */
    @NonNull
    default Mono<Boolean> hasPermission(@NonNull AssetType assetType,
                                        @NonNull Collection<?> assetId,
                                        @NonNull AssetPermission... permission) {
        return hasPermission(assetType, assetId, false, permission);
    }

    /**
     * 判定是否有指定资产的操作权限
     *
     * @param assetType          资产类型
     * @param assetId            资产ID集合
     * @param permission         权限
     * @param allowAssetNotExist 如果资产不存在,是否返回true
     * @return 是否有权限
     */
    @NonNull
    Mono<Boolean> hasPermission(@NonNull AssetType assetType,
                                @NonNull Collection<?> assetId,
                                boolean allowAssetNotExist,
                                @NonNull AssetPermission... permission);

    /**
     * 判定是否有指定资产的操作权限
     *
     * @param assetType          资产类型
     * @param assetId            资产ID集合
     * @param permission         权限
     * @param allowAssetNotExist 如果资产不存在,是否返回true
     * @return 是否有权限
     */
    @NonNull
    Mono<Boolean> hasPermission(@NonNull String assetType,
                                @NonNull Collection<?> assetId,
                                boolean allowAssetNotExist,
                                @NonNull AssetPermission... permission);

    /**
     * 过滤掉没有权限的资产
     *
     * @param source        数据源
     * @param assetType     资产类型
     * @param assetIdMapper 资产ID转换器
     * @param permission    权限
     * @param <T>           资产类型
     * @return 资产列表
     */
    @NonNull <T> Flux<T> filter(@NonNull Flux<T> source,
                                @NonNull AssetType assetType,
                                @NonNull Function<T, ?> assetIdMapper,
                                @NonNull AssetPermission... permission);

    /**
     * 判定是否有指定资产的操作权限
     *
     * @param assetType  资产类型 {@link AssetType#getId()}
     * @param assetId    资产ID集合
     * @param permission 权限
     * @return 是否有权限
     */
    @NonNull
    default Mono<Boolean> hasPermission(@NonNull String assetType,
                                        @NonNull Collection<?> assetId,
                                        @NonNull AssetPermission... permission) {
        return hasPermission(assetType, assetId, false, permission);
    }


    /**
     * 断言是否有指定资产的操作权限,如果没有权限则抛出{@link AccessDenyException}
     *
     * @param assetType  资产类型
     * @param assetId    资产ID
     * @param permission 权权限
     * @return void
     * @see AccessDenyException
     */
    default @NonNull Mono<Void> assetPermission(@NonNull AssetType assetType,
                                                @NonNull Collection<?> assetId,
                                                @NonNull AssetPermission... permission) {
        return assetPermission(assetType, assetId, false, permission);
    }

    /**
     * 断言是否有指定资产的操作权限,如果没有权限则抛出{@link AccessDenyException}
     *
     * @param assetType          资产类型
     * @param assetId            资产ID
     * @param permission         权权限
     * @param allowAssetNotExist 如果资产不存在,是否返回true
     * @return void
     * @see AccessDenyException
     */
    default @NonNull Mono<Void> assetPermission(@NonNull AssetType assetType,
                                                @NonNull Collection<?> assetId,
                                                boolean allowAssetNotExist,
                                                @NonNull AssetPermission... permission) {
        return hasPermission(assetType, assetId, allowAssetNotExist, permission)
            .filter(Boolean.TRUE::equals)
            .switchIfEmpty(Mono.error(AccessDenyException::new))
            .then();
    }

    /**
     * 断言是否有指定资产的操作权限,如果没有权限则抛出{@link AccessDenyException}
     *
     * @param assetType          资产类型
     * @param assetId            资产ID
     * @param permission         权权限
     * @param allowAssetNotExist 如果资产不存在,是否返回true
     * @return void
     * @see AccessDenyException
     */
    default @NonNull Mono<Void> assetPermission(@NonNull String assetType,
                                                @NonNull Collection<?> assetId,
                                                boolean allowAssetNotExist,
                                                @NonNull AssetPermission... permission) {
        return hasPermission(assetType, assetId, allowAssetNotExist, permission)
            .filter(Boolean.TRUE::equals)
            .switchIfEmpty(Mono.error(AccessDenyException::new))
            .then();
    }

    /**
     * 断言是否有指定资产的操作权限,如果没有权限则抛出{@link AccessDenyException}
     *
     * @param assetType  资产类型
     * @param assetId    资产ID
     * @param permission 权权限
     * @return void
     * @see AccessDenyException
     */
    default @NonNull Mono<Void> assetPermission(@NonNull String assetType,
                                                @NonNull Collection<?> assetId,
                                                @NonNull AssetPermission... permission) {
        return assetPermission(assetType, assetId, false, permission);
    }


    /* ===============静态方法==================== */

    /**
     * 获取当前租户信息
     *
     * @return 租户信息
     */
    static @NonNull Mono<TenantMember> current() {
        return CurrentTenantHolder.current();
    }

    /**
     * 从认证信息中获取租户信息
     *
     * @param authentication 权限信息
     * @return 租户信息
     */
    static @NotNull Mono<TenantMember> fromAuth(Authentication authentication) {
        return CurrentTenantHolder.fromAuth(authentication);
    }

    /**
     * 从认证信息中获取全部租户信息
     *
     * @param authentication 认证信息
     * @return 租户信息
     * @since 1.7
     */
    static @NotNull Flux<TenantMember> fromAuthAll(Authentication authentication) {
        return CurrentTenantHolder
            .fromAuth(authentication)
            .flatMapIterable(member -> {
                if (member instanceof MultiTenantMember) {
                    return ((MultiTenantMember) member).getMembers();
                }
                return Collections.singleton(member);
            });
    }

    /**
     * 获取当前的全部租户成员信息（一个成员在多个租户中）
     *
     * @return 租户信息
     */
    static @NonNull Flux<TenantMember> currentAll() {
        return CurrentTenantHolder
            .current()
            .flatMapIterable(member -> {
                if (member instanceof MultiTenantMember) {
                    return ((MultiTenantMember) member).getMembers();
                }
                return Collections.singleton(member);
            });
    }


    /**
     * 断言用户拥有对资产的权限,如果用户不是租户成员则直接通过,如果没有任意一个资产信息,则抛出异常{@link AccessDenyException}
     *
     * @param source             数据源
     * @param type               资产类型
     * @param assetIdMapper      资产ID转换
     * @param allowAssetNotExist 资产不存在时,默认通过
     * @param permission         权限列表
     * @param <T>                资产数据类型
     * @return 源数据
     * @see AccessDenyException
     */
    static <T> Flux<T> assertPermission(Flux<T> source,
                                        AssetType type,
                                        Function<T, ?> assetIdMapper,
                                        boolean allowAssetNotExist,
                                        AssetPermission... permission) {
        Flux<T> cache = source.cache();
        return Mono
            .zip(TenantMember.current(), cache.collectList())
            .flatMapMany(tp2 -> tp2.getT1()
                                   .assetPermission(type, tp2.getT2()
                                                             .stream()
                                                             .map(assetIdMapper)
                                                             .collect(Collectors.toList()), allowAssetNotExist, permission))
            .thenMany(cache);
    }

    /**
     * 断言用户拥有对资产的权限,如果用户不是租户成员则直接通过,如果没有任意一个资产信息,则抛出异常{@link AccessDenyException}
     *
     * @param source             数据源
     * @param type               资产类型
     * @param assetIdMapper      资产ID转换
     * @param allowAssetNotExist 资产不存在时,默认通过
     * @param permission         权限列表
     * @param <T>                资产数据类型
     * @return 源数据
     * @see AccessDenyException
     */
    static <T> Mono<T> assertPermission(Mono<T> source,
                                        AssetType type,
                                        Function<T, ?> assetIdMapper,
                                        boolean allowAssetNotExist,
                                        AssetPermission... permission) {
        Mono<T> cache = source.cache();
        return Mono
            .zip(TenantMember.current(), cache)
            .flatMap(tp2 -> tp2.getT1()
                               .assetPermission(type, Collections.singleton(assetIdMapper.apply(tp2.getT2())), allowAssetNotExist, permission)
                               .thenReturn(tp2.getT2()))
            .switchIfEmpty(cache);
    }


    /**
     * 断言当前租户对指定数据有相应的权限
     *
     * @param source        数据源
     * @param type          资产类型
     * @param assetIdMapper 资产ID转换器
     * @param permission    权限,空数组则只不判断权限,只要有资产即通过
     * @param <T>           数据类型
     * @return 处理后的数据流
     * @see AccessDenyException
     */
    static <T> Mono<T> assertPermission(Mono<T> source,
                                        AssetType type,
                                        Function<T, ?> assetIdMapper,
                                        AssetPermission... permission) {
        return assertPermission(source, type.getId(), assetIdMapper, permission);
    }

    /**
     * 断言当前租户对指定数据有相应的权限
     *
     * @param source        数据源
     * @param type          资产类型
     * @param assetIdMapper 资产ID转换器
     * @param permission    权限,空数组则只不判断权限,只要有资产即通过
     * @param <T>           数据类型
     * @return 处理后的数据流
     * @see AccessDenyException
     */
    static <T> Mono<T> assertPermission(Mono<T> source,
                                        String type,
                                        Function<T, ?> assetIdMapper,
                                        AssetPermission... permission) {
        Mono<T> cache = source.cache();
        return Mono
            .zip(TenantMember.current(), cache)
            .flatMap(tp2 -> tp2.getT1()
                               .assetPermission(type, Collections.singleton(assetIdMapper.apply(tp2.getT2())), permission)
                               .thenReturn(tp2.getT2()))
            .switchIfEmpty(cache);
    }

    /**
     * 断言当前租户对指定数据Id的数据有相应的权限
     *
     * @param type       资产类型
     * @param id         数据ID
     * @param permission 权限,空数组则只不判断权限,只要有资产即通过
     * @return void
     * @see AccessDenyException
     */
    static Mono<Void> assertPermission(String type,
                                       Object id,
                                       AssetPermission... permission) {
        return assertPermission(Mono.just(id), type, Function.identity(), permission).then();
    }


    /**
     * 断言当前租户对指定数据Id的数据有相应的权限
     *
     * @param type       资产类型
     * @param id         数据ID
     * @param permission 权限,空数组则只不判断权限,只要有资产即通过
     * @return void
     * @see AccessDenyException
     */
    static Mono<Void> assertPermission(AssetType type,
                                       Object id,
                                       AssetPermission... permission) {
        return assertPermission(Mono.just(id), type, Function.identity(), permission).then();
    }

    /**
     * 断言当前租户对指定数据Id的数据有相应的权限
     *
     * @param type       资产类型
     * @param id         数据ID
     * @param permission 权限,空数组则只不判断权限,只要有资产即通过
     * @return void
     * @see AccessDenyException
     */
    static Mono<Void> assertPermission(AssetType type,
                                       Collection<Object> id,
                                       AssetPermission... permission) {
        return assertPermission(Flux.fromIterable(id), type, Function.identity(), permission).then();
    }


    /**
     * 断言当前租户数据源中数据的相应权限,如果当前用户不是租户,则默认通过.
     *
     * @param source        数据源
     * @param type          资产类型
     * @param assetIdMapper 资产ID转换器
     * @param permission    权限,空数组则只不判断权限,只要有资产即通过
     * @param <T>           数据类型
     * @return 处理后的数据流
     * @see AccessDenyException
     */
    static <T> Flux<T> assertPermission(Flux<T> source,
                                        AssetType type,
                                        Function<T, ?> assetIdMapper,
                                        AssetPermission... permission) {
        Flux<T> cache = source.cache();
        return Mono
            .zip(TenantMember.current(), cache.collectList())
            .flatMapMany(tp2 -> tp2.getT1()
                                   .assetPermission(type, tp2.getT2()
                                                             .stream()
                                                             .map(assetIdMapper)
                                                             .collect(Collectors.toList()), permission))
            .thenMany(cache);
    }

    /**
     * 绑定数据到资产中
     *
     * @param source        数据流
     * @param type          资产类型
     * @param assetIdMapper 资产ID转换器
     * @param <T>           数据类型
     * @return 原始数据
     */
    static <T> Flux<T> bindAssets(Flux<T> source,
                                  AssetType type,
                                  Function<T, ?> assetIdMapper) {
        Flux<T> cache = source.cache();
        return Mono
            .zip(TenantMember.current(), cache.collectList())
            .flatMapMany(tp2 -> tp2.getT1()
                                   .bindAssets(type, tp2.getT2()
                                                        .stream()
                                                        .map(assetIdMapper)
                                                        .collect(Collectors.toList())))
            .thenMany(cache);
    }


    /**
     * 给动态查询条件中注入资产数据过滤查询条件.
     *
     * @param queryParamEntity 动态查询条件
     * @param type             资产类型
     * @param property         属性
     * @return 修改后对查询条件
     */
    static Mono<QueryParamEntity> injectQueryParam(QueryParamEntity queryParamEntity,
                                                   AssetType type,
                                                   String property) {
        return injectQueryParam(queryParamEntity, type.getId(), property);
    }

    static Mono<QueryParamEntity> injectQueryParam(Mono<TenantMember> member,
                                                   QueryParamEntity queryParamEntity,
                                                   String type,
                                                   String property) {
        return member
            .map(tenant -> queryParamEntity
                .toNestQuery(query -> query.and(property,
                                                MultiAssetsTerm.ID,
                                                MultiAssetsTerm.from(type, tenant)))
                .getParam())
            .defaultIfEmpty(queryParamEntity);
    }

    /**
     * 给动态查询条件中注入资产数据过滤查询条件.
     *
     * @param queryParamEntity 动态查询条件
     * @param type             资产类型
     * @param property         属性
     * @return 修改后对查询条件
     * @see MultiAssetsTerm
     * @see AssetsTerm
     * @see MultiTenantMemberAssetsTermBuilder
     */
    static Mono<QueryParamEntity> injectQueryParam(QueryParamEntity queryParamEntity,
                                                   String type,
                                                   String property) {
        return injectQueryParam(TenantMember.current(), queryParamEntity, type, property);
    }

    /**
     * 过滤掉当前租户没有权限的资产
     *
     * @param source        数据源
     * @param assetType     资产类型
     * @param assetIdMapper 资产ID转换器
     * @param permission    权限
     * @param <T>           资产类型
     * @return 资产列表
     */
    static <T> Flux<T> filterAssets(Flux<T> source,
                                    AssetType assetType,
                                    Function<T, ?> assetIdMapper,
                                    AssetPermission... permission) {
        return TenantMember
            .current()
            .map(tenant -> tenant.filter(source, assetType, assetIdMapper, permission))
            .defaultIfEmpty(source)
            .flatMapMany(Function.identity());
    }


}
