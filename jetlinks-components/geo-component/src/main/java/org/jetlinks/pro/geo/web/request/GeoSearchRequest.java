package org.jetlinks.pro.geo.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.geo.GeoQueryParameter;
import org.jetlinks.pro.geo.GeoSearch;

@Getter
@Setter
public class GeoSearchRequest {

    public static final String document =
        "shape参数例子:<br>"
            + "根据其他对象查询: {\"objectId\":\"其他geo对象ID\",\"relation\":\"可选值:intersects,disjoint,within,contains\"}<br>"
            + "根据几何图形查询: {\"coordinates\":[],\"type\":\"可选值:Polygon,MultiPolygon\"}<br>"
        ;

    /**
     * @see GeoSearch
     */
    @Schema(description = "按指定形状查询.")
    private Object shape;

    @Schema(description = "过滤条件")
    private QueryParamEntity filter;

    public GeoQueryParameter toParameter() {
        return new GeoQueryParameter(null == shape ? null : GeoSearch.of(shape), filter);
    }
}
