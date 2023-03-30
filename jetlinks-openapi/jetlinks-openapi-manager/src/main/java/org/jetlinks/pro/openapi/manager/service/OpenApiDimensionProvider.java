package org.jetlinks.pro.openapi.manager.service;

import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.DimensionType;
import org.jetlinks.pro.openapi.manager.entity.OpenApiClientEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OpenApiDimensionProvider implements DimensionProvider {

    @Autowired
    private LocalOpenApiClientService apiClientService;

    @Override
    public Flux<? extends DimensionType> getAllType() {
        return Flux.just(OpenApiDimensionType.INSTANCE);
    }

    @Override
    public Flux<? extends Dimension> getDimensionByUserId(String userId) {
        return apiClientService.createQuery()
            .where(OpenApiClientEntity::getUserId, userId)
            .fetch()
            .map(OpenApiDimension::of);
    }

    @Override
    public Mono<? extends Dimension> getDimensionById(DimensionType type, String id) {
        if (!type.isSameType(OpenApiDimensionType.INSTANCE)) {
            return Mono.empty();
        }
        return apiClientService
            .findById(id)
            .map(OpenApiDimension::of);
    }

    @Override
    public Flux<String> getUserIdByDimensionId(String dimensionId) {
        return apiClientService
            .findById(Mono.just(dimensionId))
            .map(OpenApiClientEntity::getUserId)
            .flux();
    }
}
