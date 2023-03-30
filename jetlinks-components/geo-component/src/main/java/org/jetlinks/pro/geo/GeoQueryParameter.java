package org.jetlinks.pro.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;

import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor
@NoArgsConstructor
public class GeoQueryParameter {

    //按形状查找
    @Getter
    private GeoSearch geoSearch;

    //过滤条件
    @Getter
    @Setter
    private QueryParamEntity filter;

    public GeoQueryParameter filter(Consumer<Query<?, QueryParamEntity>> queryConsumer) {
        if (this.filter == null) {
            this.filter = new QueryParamEntity();
        }
        queryConsumer.accept(this.filter.toQuery());
        return this;
    }

    public <T> T as(Function<GeoQueryParameter, T> mapper) {
        return mapper.apply(this);
    }

    public GeoQueryParameter shape(Object shape) {
        this.geoSearch = GeoSearch.of(shape);
        return this;
    }

}
