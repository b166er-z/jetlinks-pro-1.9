package org.jetlinks.pro.openapi.manager.service.request;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.openapi.manager.entity.OpenApiClientEntity;

@Getter
@Setter
public class BindUserOpenApiClientRequest extends OpenApiClientEntity {

    private String password;

}
