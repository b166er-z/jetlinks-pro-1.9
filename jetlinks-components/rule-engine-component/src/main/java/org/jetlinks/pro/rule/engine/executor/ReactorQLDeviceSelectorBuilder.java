package org.jetlinks.pro.rule.engine.executor;

import lombok.AllArgsConstructor;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

@Component
@AllArgsConstructor
//@ConditionalOnBean(DeviceRegistry.class)
public class ReactorQLDeviceSelectorBuilder implements DeviceSelectorBuilder {

    private final DeviceRegistry registry;

    @Override
    public DeviceSelector createSelector(String expression) {

        String ql = "select * from device.selector(" + expression + ")";

        return new ReactorQLDeviceSelector(ReactorQL.builder()
            .sql(ql)
            .build(), registry);
    }


    @AllArgsConstructor
    static class ReactorQLDeviceSelector implements DeviceSelector {

        private final ReactorQL ql;

        private final DeviceRegistry registry;

        @Override
        public Flux<DeviceOperator> select(Map<String, Object> context) {
            return ql
                .start(
                    ReactorQLContext
                        .ofDatasource((r)->Flux.just(context))
                    .bindAll(context)
                )
                .map(ReactorQLRecord::asMap)
                .flatMap(res -> registry.getDevice((String) res.get("id")))
                ;
        }
    }
}
