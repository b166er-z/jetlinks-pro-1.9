package org.jetlinks.pro.visualization.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.List;

@Table(name = "vis_catalog")
@Getter
@Setter
public class VisualizationCatalogEntity extends GenericTreeSortSupportEntity<String> {

    @Column
    private String name;

    @Column
    private String description;

    private List<VisualizationCatalogEntity> children;


}
