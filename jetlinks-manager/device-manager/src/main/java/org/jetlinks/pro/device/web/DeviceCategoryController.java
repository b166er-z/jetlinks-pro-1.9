package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.pro.device.entity.DeviceCategoryEntity;
import org.jetlinks.pro.device.service.DeviceCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/device/category")
@Slf4j
@Tag(name = "产品分类管理")
@AllArgsConstructor
@Resource(id="device-category",name = "产品分类")
public class DeviceCategoryController implements ReactiveServiceCrudController<DeviceCategoryEntity,String> {


    private final DeviceCategoryService categoryService;

    @GetMapping
    @Operation(summary = "获取全部分类")
    @Authorize(merge = false)
    public Flux<DeviceCategoryEntity> getAllCategory() {
        return this
            .categoryService
            .createQuery()
            .fetch();
    }

    @GetMapping("/_tree")
    @Operation(summary = "获取全部分类(树结构)")
    @Authorize(merge = false)
    public Flux<DeviceCategoryEntity> getAllCategoryTree() {
        return this
            .categoryService
            .createQuery()
            .fetch()
            .collectList()
            .flatMapMany(all-> Flux.fromIterable(TreeSupportEntity.list2tree(all, DeviceCategoryEntity::setChildren)));
    }

    @Override
    public ReactiveCrudService<DeviceCategoryEntity, String> getService() {
        return categoryService;
    }
}
