package org.jetlinks.pro.tenant.crud;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.pro.tenant.TenantMember;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 支持租户关联资产权限控制到通用增删改查控制器
 *
 * @param <E> 实体类型
 * @param <K> 主键类型
 * @author zhouhao
 * @since 1.2
 */
public interface TenantCorrelatesAccessCrudController<E, K> extends ReactiveServiceCrudController<E, K>, TenantCorrelatesSupport<E> {

    @Override
    default Mono<SaveResult> save(@RequestBody Flux<E> payload) {
        return ReactiveServiceCrudController
            .super
            .save(TenantMember.assertPermission(payload, getAssetType(), getAssetIdMapper(), true));
    }

    @Override
    @PutMapping("/{id}")
    default Mono<Boolean> update(@PathVariable K id, @RequestBody Mono<E> payload) {
        return this
            .assertPermission(getService().findById(id))
            .flatMap(old -> ReactiveServiceCrudController.super.update(id, payload));
    }

    @Override
    default Mono<E> add(@RequestBody Mono<E> payload) {
        return ReactiveServiceCrudController.super.add(assertPermission(payload));
    }

    @Override
    default Mono<Integer> add(@RequestBody Flux<E> payload) {
        return ReactiveServiceCrudController.super.add(assertPermission(payload));
    }

    @Override
    @DeleteMapping("/{id:.+}")
    default Mono<E> delete(@PathVariable K id) {
        return this
            .assertPermission(getService().findById(id))
            .flatMap(old -> ReactiveServiceCrudController.super.delete(id));
    }

    @Override
    @GetMapping("/{id:.+}")
    default Mono<E> getById(@PathVariable K id) {
        return this.assertPermission(ReactiveServiceCrudController.super.getById(id));
    }

    @Override
    default Flux<E> query(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> TenantMember.injectQueryParam(param, getAssetType(), getAssetProperty()))
            .flatMapMany(ReactiveServiceCrudController.super::query);
    }

    @Override
    default Flux<E> query(@Parameter(hidden = true) QueryParamEntity query) {
        return ReactiveServiceCrudController.super
            .query(TenantMember.injectQueryParam(query, getAssetType(), getAssetProperty()));
    }

    @Override
    default Mono<PagerResult<E>> queryPager(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> TenantMember.injectQueryParam(param, getAssetType(), getAssetProperty()))
            .flatMap(ReactiveServiceCrudController.super::queryPager);
    }

    @Override
    default Mono<PagerResult<E>> queryPager(@Parameter(hidden = true) QueryParamEntity query) {
        return TenantMember
            .injectQueryParam(query, getAssetType(), getAssetProperty())
            .flatMap(ReactiveServiceCrudController.super::queryPager);
    }

    @Override
    default Mono<Integer> count(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> TenantMember.injectQueryParam(param, getAssetType(), getAssetProperty()))
            .flatMap(ReactiveServiceCrudController.super::count);
    }

    @Override
    default Mono<Integer> count(@Parameter(hidden = true) QueryParamEntity query) {
        return TenantMember.injectQueryParam(query, getAssetType(), getAssetProperty())
            .flatMap(ReactiveServiceCrudController.super::count);
    }

}
