package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.metadata.types.GeoPoint;

import java.util.Map;

/**
 * 矩形查询
 * <pre>
 *  left - - - - - -
 *  |               |
 *  |               |
 *  |               |
 *   - - - - - - right
 *  </pre>
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class BoxSearch implements GeoSearch {

    private static final long serialVersionUID = 5215611530535947924L;

    /**
     * 左上角坐标
     */
    private GeoPoint left;

    /**
     * 右下角坐标
     */
    private GeoPoint right;

    public static BoxSearch of(Map<String, Object> shape) {

        return of(GeoPoint.of(shape.get("left")), GeoPoint.of(shape.get("right")));
    }
}
