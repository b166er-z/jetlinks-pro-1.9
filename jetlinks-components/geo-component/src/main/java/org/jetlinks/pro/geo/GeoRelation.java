package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Getter
public enum GeoRelation implements EnumDict<String> {
    INTERSECTS("intersects"),
    DISJOINT("disjoint"),
    WITHIN("within"),
    CONTAINS("contains");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    public static Optional<GeoRelation> of(String rel){
        return Arrays.stream(values())
            .filter(geoRelation -> geoRelation.getValue().equalsIgnoreCase(rel))
            .findFirst();
    }
}
