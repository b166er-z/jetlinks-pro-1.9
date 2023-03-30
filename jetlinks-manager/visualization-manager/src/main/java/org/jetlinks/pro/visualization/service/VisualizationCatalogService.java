package org.jetlinks.pro.visualization.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.ReactiveTreeSortEntityService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.pro.visualization.entity.VisualizationCatalogEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisualizationCatalogService extends GenericReactiveCrudService<VisualizationCatalogEntity,String>
    implements ReactiveTreeSortEntityService<VisualizationCatalogEntity,String> {

    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.SNOW_FLAKE_STRING;
    }

    @Override
    public void setChildren(VisualizationCatalogEntity entity, List<VisualizationCatalogEntity> children) {
        entity.setChildren(children);
    }


}
