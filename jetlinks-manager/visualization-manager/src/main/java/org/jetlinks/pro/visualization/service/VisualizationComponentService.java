package org.jetlinks.pro.visualization.service;

import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.pro.visualization.entity.VisualizationComponentEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisualizationComponentService extends GenericReactiveTreeSupportCrudService<VisualizationComponentEntity, String> {
    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.SNOW_FLAKE_STRING;
    }

    @Override
    public void setChildren(VisualizationComponentEntity entity, List<VisualizationComponentEntity> children) {
        entity.setChildren(children);
    }
}
