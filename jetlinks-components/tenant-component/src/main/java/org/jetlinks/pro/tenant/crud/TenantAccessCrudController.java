package org.jetlinks.pro.tenant.crud;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 支持租户权限控制到通用增删改查控制器
 *
 * @param <E> 实体类型
 * @param <K> 主键类型
 * @author zhouhao
 * @since 1.2
 */
public interface TenantAccessCrudController<E, K> extends ReactiveServiceCrudController<E, K> {

    @Override
    @TenantAssets(ignore = true)
    default E applyAuthentication(E entity, Authentication authentication) {
        return ReactiveServiceCrudController.super.applyAuthentication(entity, authentication);
    }

    @Override
    @TenantAssets(ignore = true)
    default E applyCreationEntity(Authentication authentication, E entity) {
        return ReactiveServiceCrudController.super.applyCreationEntity(authentication, entity);
    }

    @Override
    @TenantAssets(ignore = true)
    default E applyModifierEntity(Authentication authentication, E entity) {
        return ReactiveServiceCrudController.super.applyModifierEntity(authentication, entity);
    }

    @Override
    @TenantAssets(assetObjectIndex = 0, autoBind = true, allowAssetNotExist = true)
    default Mono<SaveResult> save(@RequestBody Flux<E> payload) {
        return ReactiveServiceCrudController.super.save(payload);
    }

    @PutMapping("/{id}")
    @Override
    @TenantAssets
    default Mono<Boolean> update(@PathVariable K id, @RequestBody Mono<E> payload) {
        return ReactiveServiceCrudController.super.update(id, payload);
    }

    @Override
    @TenantAssets(assetObjectIndex = 0, validate = false, autoBind = true)
    default Mono<E> add(@RequestBody Mono<E> payload) {
        return ReactiveServiceCrudController.super.add(payload);
    }

    @Override
    @TenantAssets(assetObjectIndex = 0, validate = false, autoBind = true)
    default Mono<Integer> add(@RequestBody Flux<E> payload) {
        return ReactiveServiceCrudController.super.add(payload);
    }

    @Override
    @DeleteMapping("/{id:.+}")
    @TenantAssets(autoUnbind = true)
    default Mono<E> delete(@PathVariable K id) {
        return ReactiveServiceCrudController.super.delete(id);
    }

    @Override
    @GetMapping("/{id:.+}")
    @TenantAssets(allowAssetNotExist = true)
    default Mono<E> getById(@PathVariable K id) {
        return ReactiveServiceCrudController.super.getById(id);
    }
}
