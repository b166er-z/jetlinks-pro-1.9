package org.jetlinks.pro.elastic.search.utils;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexMetadata;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchConverter {


    public static SearchSourceBuilder convertSearchSourceBuilder(QueryParam queryParam, ElasticSearchIndexMetadata metadata) {
        return QueryParamTranslator.convertSearchSourceBuilder(queryParam, metadata);
    }

    public static Map<String, Object> convertDataToElastic(Map<String, Object> data, List<PropertyMetadata> properties) {
        for (PropertyMetadata property : properties) {
            DataType type = property.getValueType();
            Object val = data.get(property.getId());
            if (val == null) {
                continue;
            }
            //处理地理位置类型
            if (type instanceof GeoType) {
                GeoPoint point = ((GeoType) type).convert(val);

                Map<String, Object> geoData = new HashMap<>();
                geoData.put("lat", point.getLat());
                geoData.put("lon", point.getLon());

                data.put(property.getId(), geoData);
            } else if (type instanceof GeoShapeType) {
                GeoShape shape = ((GeoShapeType) type).convert(val);
                if (shape == null) {
                    throw new UnsupportedOperationException("不支持的GeoShape格式:" + val);
                }
                Map<String, Object> geoData = new HashMap<>();
                geoData.put("type", shape.getType().name());
                geoData.put("coordinates", shape.getCoordinates());
                data.put(property.getId(), geoData);
            } else if (type instanceof DateTimeType) {
                Date date = ((DateTimeType) type).convert(val);
                data.put(property.getId(), date.getTime());
            } else if (type instanceof Converter) {
                data.put(property.getId(), ((Converter<?>) type).convert(val));
            }
        }
        return data;
    }

    public static Map<String, Object> convertDataFromElastic(Map<String, Object> data, List<PropertyMetadata> properties) {
        for (PropertyMetadata property : properties) {
            DataType type = property.getValueType();
            Object val = data.get(property.getId());
            if (val == null) {
                continue;
            }
            //处理地理位置类型
            if (type instanceof GeoType) {
                data.put(property.getId(), ((GeoType) type).convertToMap(val));
            } else if (type instanceof DateTimeType) {
                Date date = ((DateTimeType) type).convert(val);
                data.put(property.getId(), date);
            } else if (type instanceof Converter) {
                data.put(property.getId(), ((Converter<?>) type).convert(val));
            }
        }
        return data;
    }
}
