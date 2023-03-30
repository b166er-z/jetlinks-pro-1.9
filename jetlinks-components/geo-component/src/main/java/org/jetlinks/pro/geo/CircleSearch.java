package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.metadata.types.GeoPoint;

import java.util.Map;

/**
 * 圆形
 *
 * @author zhouhao
 * @since 1.1
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class CircleSearch implements GeoSearch {
    private static final long serialVersionUID = 5215611530535947924L;

    /**
     * 圆形中心点
     */
    private GeoPoint center;

    /**
     * 半径
     */
    private Distance radius;

    public static CircleSearch of(Map<String, Object> map) {
        return new CircleSearch(GeoPoint.of(map.get("center")), Distance.of((String) map.get("radius")));
    }
}
