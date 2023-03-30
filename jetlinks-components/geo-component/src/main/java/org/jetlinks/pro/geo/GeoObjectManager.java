package org.jetlinks.pro.geo;

import org.hswebframework.web.api.crud.entity.PagerResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Geo操作接口,提供Geo支持
 *
 * @author zhouhao
 * @since 1.1
 */
public interface GeoObjectManager {

    /**
     * 查找geoObject
     *
     * @param param 查询条件
     * @return GeoObject 结果流
     */
    Mono<PagerResult<GeoObject>> findPager(GeoQueryParameter param);

    /**
     * 查询object,不返回分页结果
     *
     * @param param 查询条件
     * @return 结果流
     */
    Flux<GeoObject> find(GeoQueryParameter param);

    /**
     * 添加一个GeoObject,如果已经存在则覆盖.
     *
     * @param geoObject GeoObject
     * @return Mono
     */
    Mono<Void> put(GeoObject geoObject);

    /**
     * 添加多个GeoObject,如果已经存在则覆盖.
     *
     * @param geoObject GeoObject
     * @return Mono
     */
    Mono<Void> put(Collection<GeoObject> geoObject);

    Mono<Void> commit(Collection<GeoObject> geoObject);

    /**
     * 删除GeoObject
     *
     * @param id ID {@link GeoObject#getId()}
     * @return Mono
     */
    Mono<Void> remove(String id);

    /**
     * 根据查询参数删除geoObject
     * @param parameter 参数
     * @return Mono
     */
    Mono<Long> remove(GeoQueryParameter parameter);

    /**
     * 删除多个GeoObject
     *
     * @param idList ID {@link GeoObject#getId()}
     * @return Mono
     */
    Mono<Void> remove(Collection<String> idList);

}
