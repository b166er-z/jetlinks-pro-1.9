package org.jetlinks.pro.geo.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.types.GeoShape;
import org.jetlinks.pro.geo.GeoObject;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class GeoJsonFeature {

    private String type = "Feature";

    @Schema(description = "配置: id,objectType,objectId不能为空")
    private Map<String, Object> properties = new HashMap<>();

    private GeoShape geometry;

    public static GeoJsonFeature of(GeoObject object) {
        GeoJsonFeature feature = new GeoJsonFeature();
        feature.geometry = object.getShape();
        if (!CollectionUtils.isEmpty(object.getTags())) {
            feature.properties.putAll(object.getTags());
        }
        feature.properties.put("objectId", object.getObjectId());
        feature.properties.put("id", object.getId());
        feature.properties.put("objectType", object.getObjectType());
        return feature;

    }

    public GeoObject toGeoObject() {
        GeoObject object = new GeoObject();
        object.setShape(geometry);
        object.setObjectId(String.valueOf(Objects.requireNonNull(properties.remove("objectId"), "objectId不能为空")));
        object.setObjectType(String.valueOf(Objects.requireNonNull(properties.remove("objectType"), "objectType不能为空")));
        object.setId(String.valueOf(Objects.requireNonNull(properties.remove("id"), "id不能为空")));
        object.setTags(properties);
        return object;
    }

}
