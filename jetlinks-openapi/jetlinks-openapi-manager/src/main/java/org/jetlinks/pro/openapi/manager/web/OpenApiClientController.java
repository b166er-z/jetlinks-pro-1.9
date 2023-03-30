package org.jetlinks.pro.openapi.manager.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.jetlinks.pro.openapi.manager.entity.OpenApiClientEntity;
import org.jetlinks.pro.openapi.manager.enums.DataStatus;
import org.jetlinks.pro.openapi.manager.service.LocalOpenApiClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("/open-api")
@Resource(id = "open-api", name = "openApi客户端")
@Tag(name = "OpenAPI客户端管理")
public class OpenApiClientController implements ReactiveServiceCrudController<OpenApiClientEntity, String> {

    @Autowired
    public LocalOpenApiClientService openApiClientService;

    @Override
    public ReactiveCrudService<OpenApiClientEntity, String> getService() {
        return openApiClientService;
    }

}
