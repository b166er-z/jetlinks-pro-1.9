package org.jetlinks.pro.geo.web;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GeoJson {

    private String type = "FeatureCollection";

    private List<GeoJsonFeature> features;

    public GeoJson(List<GeoJsonFeature> features) {
        this.features = features;
    }

}
