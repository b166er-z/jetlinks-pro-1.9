package org.jetlinks.pro.elastic.search.geo;

import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.elastic.search.service.ElasticSearchService;
import org.jetlinks.pro.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.jetlinks.pro.geo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

@Component
public class ElasticSearchGeoObjectManager implements GeoObjectManager {

    static String index = "geo-manager";

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Autowired
    private ReactiveElasticsearchClient reactiveElasticsearchClient;

    @Override
    public Mono<PagerResult<GeoObject>> findPager(GeoQueryParameter param) {
        return elasticSearchService
            .queryPager(index, createQueryParamEntity(param), map -> FastBeanCopier.copy(map, new GeoObject()));
    }

    private QueryParamEntity createQueryParamEntity(GeoQueryParameter param) {
        QueryParamEntity entity = param.getFilter() == null ? new QueryParamEntity() : param.getFilter();
        if (param.getGeoSearch() != null) {
            GeoSearch geoSearch = param.getGeoSearch();
            if (geoSearch instanceof ObjectSearch) {
                geoSearch = GeoIndexedShapeSearch.of(index, ((ObjectSearch) geoSearch).getObjectId(), "shape", geoSearch.getRelation());
            }
            if (geoSearch instanceof CircleSearch) { //只支持查询坐标
                entity.and("point", "geo", geoSearch);
            } else {
                //地形查找
                entity.and("shape", "geo-shape", geoSearch);
            }
        }
        return entity;
    }

    @Override
    public Flux<GeoObject> find(GeoQueryParameter param) {
        return elasticSearchService
            .query(index, createQueryParamEntity(param), map -> FastBeanCopier.copy(map, new GeoObject()));
    }

    @Override
    public Mono<Void> put(GeoObject geoObject) {
        return elasticSearchService.save(index, geoObject);
    }

    @Override
    public Mono<Void> put(Collection<GeoObject> geoObject) {
        return elasticSearchService.save(index, geoObject);
    }

    @Override
    public Mono<Void> commit(Collection<GeoObject> geoObject) {
        return elasticSearchService.commit(index, geoObject);
    }

    @Override
    public Mono<Void> remove(String id) {
        return remove(Collections.singletonList(id));
    }

    @Override
    public Mono<Long> remove(GeoQueryParameter parameter) {
        return elasticSearchService.delete(index, createQueryParamEntity(parameter));
    }

    @Override
    public Mono<Void> remove(Collection<String> idList) {

        return Flux
            .fromIterable(idList)
            .map(id -> new DeleteRequest(index, "_doc", id))
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> {
                BulkRequest request = new BulkRequest();
                list.forEach(request::add);
                return reactiveElasticsearchClient
                    .bulk(request)
                    .then();
            });
    }
}
