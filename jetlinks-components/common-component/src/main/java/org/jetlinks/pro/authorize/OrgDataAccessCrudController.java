package org.jetlinks.pro.authorize;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.define.Phased;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author wangzheng
 * @see ReactiveServiceCrudController,OrgDataAccess
 * @since 1.0
 */
@OrgDataAccess
@Deprecated
public interface OrgDataAccessCrudController<E,K> extends ReactiveServiceCrudController<E,K> {

    @OrgDataAccess(idParamIndex = -1)
    @Override
    default Mono<SaveResult> save(@RequestBody Flux<E> payload) {
        return ReactiveServiceCrudController.super.save(payload);
    }

    @OrgDataAccess(idParamIndex = -1)
    @Override
    default Mono<E> add(@RequestBody Mono<E> payload) {
        return ReactiveServiceCrudController.super.add(payload);
    }

    @OrgDataAccess
    @Override
    default Mono<Boolean> update(@PathVariable K id, @RequestBody Mono<E> payload) {
        return ReactiveServiceCrudController.super.update(id, payload);
    }

    @OrgDataAccess(phased = Phased.after)
    @Override
    default Mono<E> getById(@PathVariable K id) {
        return ReactiveServiceCrudController.super.getById(id);
    }
}
