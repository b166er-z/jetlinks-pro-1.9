package org.jetlinks.pro.openapi.manager.service;

import org.hswebframework.web.authorization.DimensionType;

public class OpenApiDimensionType implements DimensionType {
    public static OpenApiDimensionType INSTANCE = new OpenApiDimensionType();

    @Override
    public String getId() {
        return "open-api";
    }

    @Override
    public String getName() {
        return "OpenAPI";
    }
}
