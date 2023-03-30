package org.jetlinks.pro.geo.web;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.authorization.annotation.*;
import org.jetlinks.pro.geo.GeoObject;
import org.jetlinks.pro.geo.GeoObjectManager;
import org.jetlinks.pro.geo.GeoSearch;
import org.jetlinks.pro.geo.web.request.GeoSearchRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Geo对象管理接口
 *
 * @author zhouhao
 * @since 1.1
 */
@RestController
@RequestMapping({"/geo/object", "/api/v1/geo/object"})
@Resource(id = "geo-manager", name = "地理信息管理")
@Tag(name = "地理位置信息管理")
public class GeoObjectController {

    public final GeoObjectManager manager;

    public GeoObjectController(GeoObjectManager manager) {
        this.manager = manager;
    }

    //查询geo object
    @PostMapping("/_search")
    @ResourceAction(id = "find-geo", name = "查询")
    @Operation(
        summary = "查询地理位置对象",
        description = GeoSearchRequest.document
    )
    public Flux<GeoObject> findGeoObject(@RequestBody Mono<GeoSearchRequest> request) {
        return request
            .map(GeoSearchRequest::toParameter)
            .flatMapMany(param -> {
                boolean excludeShape = null != param.getFilter()
                    &&
                    param.getFilter().getExcludes().contains("shape");
                return manager
                    .find(param)
                    .doOnNext(geo -> {
                        if (excludeShape) {
                            geo.setShape(null);
                        }
                    });
            });
    }


    //分页查询geo object
    @PostMapping("/_search/_page")
    @ResourceAction(id = "find-geo", name = "查询")
    @Operation(
        summary = "分页查询地理位置对象",
        description = GeoSearchRequest.document
    )
    public Mono<PagerResult<GeoObject>> findGeoObjectPage(@RequestBody Mono<GeoSearchRequest> request) {
        return request
            .map(GeoSearchRequest::toParameter)
            .flatMap(manager::findPager);
    }


    /**
     * 查询geo对象并转为geoJson
     *
     * <pre>
     *     POST /geo/object/_search/geo.json
     *
     *     {
     *         "shape":{
     *             "objectId":"chongqing" //查询id为chongqing geo对象内的所有geo信息.更多查询查看{@link GeoSearch}
     *         },
     *         "filter":{
     *
     *         }
     *     }
     *
     * </pre>
     *
     * @param request 查询请求
     * @return GeoJson
     */
    @PostMapping("/_search/geo.json")
    @ResourceAction(id = "find-geo", name = "查询")
    @Operation(
        summary = "查询地理位置对象返回geoJson格式",
        description = GeoSearchRequest.document
    )
    public Mono<GeoJson> findGeoObjectToGeoJson(@RequestBody Mono<GeoSearchRequest> request) {
        return request
            .map(GeoSearchRequest::toParameter)
            .flatMapMany(manager::find)
            .map(GeoJsonFeature::of)
            .collectList()
            .map(GeoJson::new);
    }

    /**
     * 根据geo json保存数据. 可使用 http://geojson.io/#map=4/32.18/105.38 生成json
     * <pre>
     *  POST /geo/object/geo.json
     *
     *  {
     *   "type": "FeatureCollection",
     *   "features": [
     *     {
     *       "type": "Feature",
     *       "properties": { //拓展属性
     *
     *           //必须的配置
     *           "id":"chongqing",
     *           "objectId":"chongqing",
     *           "objectType":"city",
     *
     *           //其他配置,将设置到 GeoObject.tags中,在查询时可通过filter进行搜索.
     *           "group":"china",
     *           "name":"重庆市"
     *       },
     *       "geometry": {
     *         "type": "Polygon",
     *         "coordinates": [
     *           [
     *              //坐标列表
     *           ]
     *         ]
     *       }
     *     }
     *   ]
     * }
     *
     *
     * </pre>
     *
     * @param request JSON
     * @return 保存结果
     */
    @PostMapping("/geo.json")
    @ResourceAction(id = "save-geo", name = "保存")
    @Operation(
        summary = "按照geojson格式保存geo对象"
    )
    public Mono<Void> saveByGeoJson(@RequestBody Mono<GeoJson> request) {
        return request
            .flatMapMany(json -> Flux.fromIterable(json.getFeatures()))
            .map(GeoJsonFeature::toGeoObject)
            .collectList()
            .flatMap(manager::put);
    }

    /**
     * 根据ID删除Geo对象
     * <pre>
     *     DELETE /geo/object/{id}
     * </pre>
     *
     * @param id ID
     * @return 删除结果
     * @since 1.2
     */
    @DeleteMapping("/{id}")
    @DeleteAction
    @Operation(
        summary = "根据ID删除地理位置对象"
    )
    public Mono<Void> deleteById(@PathVariable String id) {
        return manager.remove(id);
    }

    /**
     * 根据查询参数删除对象
     *
     * <pre>
     * POST /geo/object/_delete
     * {
     * "filter":{ "where":"objectId = test" }
     * }
     * </pre>
     *
     * @param request 查询参数
     * @return 删除的记录数量
     * @since 1.2
     */
    @PostMapping("/_delete")
    @DeleteAction
    @Operation(
        summary = "根据查询参数删除geo对象"
    )
    public Mono<Long> delete(@RequestBody Mono<GeoSearchRequest> request) {
        return request
            .map(GeoSearchRequest::toParameter)
            .flatMap(manager::remove);
    }

}
