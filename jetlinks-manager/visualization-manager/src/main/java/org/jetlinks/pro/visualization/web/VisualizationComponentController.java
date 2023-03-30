package org.jetlinks.pro.visualization.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveCrudController;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.crud.web.reactive.ReactiveTreeServiceQueryController;
import org.jetlinks.pro.visualization.entity.VisualizationComponentEntity;
import org.jetlinks.pro.visualization.service.VisualizationComponentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/visualization-component")
@Resource(id = "visualization-component", name = "可视化组件管理")
@Tag(name = "可视化组件管理")
public class VisualizationComponentController implements
    ReactiveServiceCrudController<VisualizationComponentEntity, String>,
    ReactiveTreeServiceQueryController<VisualizationComponentEntity,String> {

    private final VisualizationComponentService service;

    public VisualizationComponentController(VisualizationComponentService service) {
        this.service = service;
    }

    @Override
    public VisualizationComponentService getService() {
        return service;
    }

    @GetMapping("/{type}.json")
    @Authorize(merge = false)
    public Flux<VisualizationComponentEntity> findComponentsByType(@PathVariable String type) {
        return service
            .createQuery()
            .where(VisualizationComponentEntity::getType, type)
            .orderBy(SortOrder.asc(VisualizationComponentEntity::getSortIndex))
            .fetch();
    }

}
