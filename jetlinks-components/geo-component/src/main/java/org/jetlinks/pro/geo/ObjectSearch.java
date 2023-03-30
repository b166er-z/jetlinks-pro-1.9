package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 根据另外一个对象ID查询, 场景: 查询重庆市内所有设备.
 *
 * @author zhouhao
 * @since 1.1
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ObjectSearch implements GeoSearch {

    /**
     * 对象ID
     *
     * @see GeoObject#getId()
     */
    private String objectId;

    private GeoRelation relation;

    public static ObjectSearch of(Map<String, Object> map) {
        return of(String.valueOf(map.get("objectId")), GeoRelation.of((String) map.get("relation")).orElse(null));
    }
}
