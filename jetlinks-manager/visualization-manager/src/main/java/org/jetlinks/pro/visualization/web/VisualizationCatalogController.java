package org.jetlinks.pro.visualization.web;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.crud.web.reactive.ReactiveTreeServiceQueryController;
import org.jetlinks.pro.visualization.entity.VisualizationCatalogEntity;
import org.jetlinks.pro.visualization.service.VisualizationCatalogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visualization/catalog")
@Resource(id = "visualization", name = "可视化管理")
@AllArgsConstructor
public class VisualizationCatalogController implements
    ReactiveTreeServiceQueryController<VisualizationCatalogEntity, String>,
    ReactiveServiceCrudController<VisualizationCatalogEntity, String> {

    private final VisualizationCatalogService catalogService;

    @Override
    public VisualizationCatalogService getService() {
        return catalogService;
    }
}
