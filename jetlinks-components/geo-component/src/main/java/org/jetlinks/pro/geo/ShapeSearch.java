package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.metadata.types.GeoShape;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ShapeSearch implements GeoSearch {

    private GeoShape shape;

    public static ShapeSearch of(Map<String, Object> shape) {
        return ShapeSearch.of(GeoShape.of(shape));
    }
}
