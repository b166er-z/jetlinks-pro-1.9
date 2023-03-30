package org.jetlinks.pro.device.features;

import lombok.SneakyThrows;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.jetlinks.pro.utils.TimeUtils;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import java.util.Date;

/**
 *
 * 获取设备属性信息
 *
 * @author zhouhao
 */
public class DevicePropertyAggMapFeature extends FunctionMapFeature {

    public DevicePropertyAggMapFeature(String id,
                                       Aggregation agg,
                                       Function3<String,
                                           DeviceDataService.AggregationRequest,
                                           DeviceDataService.DevicePropertyAggregation,
                                           Flux<AggregationData>> executor) {
        super(id, 4, 4, args -> args
            .collectList()
            .flatMap(list -> {
                String arg0 = String.valueOf(list.get(0));
                String property = String.valueOf(list.get(1));
                Object fromObj = list.get(2);
                Object toObj = list.get(3);

                Date from, to;

                try {
                    from = CastUtils.castDate(fromObj);
                } catch (Throwable e) {
                    from = TimeUtils.parseDate(String.valueOf(fromObj));
                }
                try {
                    to = CastUtils.castDate(toObj);
                } catch (Throwable e) {
                    to = TimeUtils.parseDate(String.valueOf(toObj));
                }

                DeviceDataService.AggregationRequest request = new DeviceDataService.AggregationRequest();
                request.setLimit(1);
                request.setFrom(from);
                request.setTo(to);
                request.setFormat("yyyy-MM-dd HH:mm:ss");
                request.setInterval(null);

                DeviceDataService.DevicePropertyAggregation aggregation = new DeviceDataService.DevicePropertyAggregation();
                aggregation.setProperty(property);
                aggregation.setAgg(agg);

                return executor.apply(arg0, request, aggregation)
                    .take(1)
                    .singleOrEmpty()
                    .flatMap(data -> Mono.justOrEmpty(data.get(property)));
            }));
    }


}
