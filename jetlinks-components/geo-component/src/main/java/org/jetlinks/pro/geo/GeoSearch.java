package org.jetlinks.pro.geo;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;
import java.util.Map;


/**
 * Geo 查询统一接口定义
 *
 * @author zhouhao
 * @see ObjectSearch
 * @see BoxSearch
 * @see CircleSearch
 * @see ShapeSearch
 * @see PolygonSearch
 * @since 1.1
 */
public interface GeoSearch extends Serializable {

    default GeoRelation getRelation() {
        return GeoRelation.WITHIN;
    }

    default GeoRelation getRelation(GeoRelation defaultValue) {
        return getRelation() == null ? defaultValue : getRelation();
    }

    static GeoSearch of(Object obj) {
        if (obj instanceof GeoSearch) {
            return ((GeoSearch) obj);
        }
        if (obj instanceof String) {
            if (((String) obj).startsWith("{")) {
                obj = JSON.parseObject(String.valueOf(obj));
            }
        }
        if (obj instanceof Map) {
            @SuppressWarnings("all")
            Map<String, Object> shape = ((Map<String, Object>) obj);
            if (shape.containsKey("left")) {
                return BoxSearch.of(shape);
            } else if (shape.containsKey("center")) {
                return CircleSearch.of(shape);
            } else if (shape.containsKey("objectId")) {
                return ObjectSearch.of(shape);
            } else if (shape.containsKey("coordinates")) {
                return ShapeSearch.of(shape);
            } else if (shape.containsKey("points")) {
                return PolygonSearch.of(shape);
            }
        }
        return null;
    }

}
