package org.jetlinks.pro.tenant.crud;

import io.swagger.v3.oas.annotations.Parameter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.pro.tenant.TenantMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 支持租户关联资产权限控制到通用查询控制器
 *
 * @param <E> 实体类型
 * @param <K> 主键类型
 * @author zhouhao
 * @since 1.2
 */
public interface TenantCorrelatesAccessQueryController<E, K> extends ReactiveServiceQueryController<E, K>, TenantCorrelatesSupport<E> {

    @Override
    @GetMapping("/{id:.+}")
    default Mono<E> getById(@PathVariable K id) {
        return this.assertPermission(ReactiveServiceQueryController.super.getById(id));
    }

    @Override
    default Flux<E> query(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> TenantMember.injectQueryParam(param, getAssetType(), getAssetProperty()))
            .flatMapMany(ReactiveServiceQueryController.super::query);
    }

    @Override
    default Flux<E> query(@Parameter(hidden = true) QueryParamEntity query) {
        return ReactiveServiceQueryController.super
            .query(TenantMember.injectQueryParam(query, getAssetType(), getAssetProperty()));
    }

    @Override
    default Mono<PagerResult<E>> queryPager(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> TenantMember.injectQueryParam(param, getAssetType(), getAssetProperty()))
            .flatMap(ReactiveServiceQueryController.super::queryPager);
    }

    @Override
    default Mono<PagerResult<E>> queryPager(@Parameter(hidden = true) QueryParamEntity query) {
        return TenantMember
            .injectQueryParam(query, getAssetType(), getAssetProperty())
            .flatMap(ReactiveServiceQueryController.super::queryPager);
    }

    @Override
    default Mono<Integer> count(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> TenantMember.injectQueryParam(param, getAssetType(), getAssetProperty()))
            .flatMap(ReactiveServiceQueryController.super::count);
    }

    @Override
    default Mono<Integer> count(@Parameter(hidden = true) QueryParamEntity query) {
        return TenantMember.injectQueryParam(query, getAssetType(), getAssetProperty())
            .flatMap(ReactiveServiceQueryController.super::count);
    }

}
