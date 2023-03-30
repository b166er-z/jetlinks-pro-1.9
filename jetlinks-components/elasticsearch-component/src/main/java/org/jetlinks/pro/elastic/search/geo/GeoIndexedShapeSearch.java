package org.jetlinks.pro.elastic.search.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.pro.geo.GeoRelation;
import org.jetlinks.pro.geo.GeoSearch;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class GeoIndexedShapeSearch implements GeoSearch {
    private String index;

    private String id;

    private String path;

    private GeoRelation relation;

}
