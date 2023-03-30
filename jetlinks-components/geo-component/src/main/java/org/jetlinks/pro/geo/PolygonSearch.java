package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.metadata.types.GeoPoint;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多边形
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class PolygonSearch implements GeoSearch {

    private List<GeoPoint> points;

    public static PolygonSearch of(Map<String, Object> shape) {
        return PolygonSearch.of(
            ((Collection<?>) shape.get("points"))
                .stream()
                .map(GeoPoint::of)
                .collect(Collectors.toList())
        );
    }
}
