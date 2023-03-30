package org.jetlinks.pro.openapi.manager.service;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.jetlinks.pro.openapi.manager.entity.OpenApiClientEntity;

import java.util.Map;

@Getter
@Setter
public class OpenApiDimension implements Dimension {
    private String id;

    private String name;

    private Map<String,Object> options;
    @Override
    public DimensionType getType() {
        return OpenApiDimensionType.INSTANCE;
    }

    public static OpenApiDimension of(OpenApiClientEntity entity){
        OpenApiDimension dimension=new OpenApiDimension();

        dimension.setId(entity.getId());
        dimension.setName(entity.getClientName());
        return dimension;

    }
}
