package org.jetlinks.pro.device.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.jetlinks.pro.authorize.OrgDataAccess;
import org.jetlinks.pro.device.entity.DeviceEvent;
import org.jetlinks.pro.device.entity.DeviceOperationLogEntity;
import org.jetlinks.pro.device.entity.DevicePropertiesEntity;
import org.jetlinks.pro.device.service.LocalDeviceProductService;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@Resource(id = "device-api-product", name = "设备数据API(根据设备型号)")
@RequestMapping("/api/v1/product")
@DeviceAsset
@Slf4j
@Tag(name = "设备数据API")
public class DeviceProductApiVer1 {

    @Autowired
    private LocalDeviceProductService productService;

    @Autowired
    private DeviceDataService deviceDataService;

    /**
     * 根据产品ID和动态查询参数查询设备相关数据
     *
     * @param productId 产品ID
     * @param query     查询参数
     * @return 查询结果
     */
    @PostMapping("/{productId}/log/_query")
    @ResourceAction(id = "query-device-log", name = "根据产品ID和动态查询参数查询设备相关数据")
    @Deprecated
    @Operation(summary = "根据产品ID和动态查询参数查询设备相关数据")
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(@PathVariable String productId,
                                                                      @RequestBody Mono<QueryParamEntity> query) {
        log.warn("接口[/api/v1/product/{}/log/_query]已弃用!", productId);
        return query.flatMap(param -> productService.queryDeviceLog(productId, param));
    }

    /**
     * 分页查询设备属性
     *
     * <pre>
     *     POST /api/v1/product/{deviceId}/properties/_query
     *
     *     {
     *         "pageSize":25,
     *         "pageIndex":0,
     *         "terms":[
     *            {
     *              "column":"property",
     *              "value":"temp"
     *            }
     *         ]
     *     }
     * </pre>
     *
     * @param productId 设备ID
     * @param query     查询条件
     * @return 分页查询结果
     */
    @PostMapping("/{productId}/properties/_query")
    @ResourceAction(id = "query-device-properties", name = "分页查询设备属性")
    @Deprecated
    @Operation(summary = "根据产品ID查询属性")
    public Mono<PagerResult<DevicePropertiesEntity>> queryDeviceProperties(@PathVariable String productId,
                                                                           @RequestBody Mono<QueryParamEntity> query) {
        log.warn("接口[/api/v1/product/{}/properties/_query]已弃用!", productId);
        return query.flatMap(param -> productService.queryDeviceProperties(productId, param));
    }


    /**
     * 查询设备事件
     *
     * <pre>
     *      POST /api/v1/product/{productId}/event/{eventId}/_query
     *
     *      {
     *        "pageSize":25,
     *        "pageIndex":0,
     *        "terms":[
     *              {
     *                 "column":"deviceId",
     *                 "value":"temp"
     *              }
     *          ]
     *      }
     * </pre>
     *
     * @param productId 设备ID
     * @param eventId   事件ID
     * @param query     查询条件
     * @return 查询结果
     */
    @PostMapping("/{productId}/event/{eventId}/_query")
    @ResourceAction(id = "query-device-events", name = "查询设备事件")
    @Operation(summary = "根据产品ID查询事件数据")
    public Mono<PagerResult<DeviceEvent>> queryDeviceEvents(
        @PathVariable String productId,
        @PathVariable String eventId,
        @RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> deviceDataService.queryEventPageByProductId(productId, eventId, param, false));
    }

    /**
     * 查询设备事件并返回格式化的数据
     *
     * <pre>
     *      POST /api/v1/product/{productId}/event/{eventId}/_query/_format
     *
     *      {
     *        "pageSize":25,
     *        "pageIndex":0,
     *        "terms":[
     *              {
     *                 "column":"deviceId",
     *                 "value":"temp"
     *              }
     *          ]
     *      }
     * </pre>
     *
     * @param productId 设备ID
     * @param eventId   事件ID
     * @param query     查询条件
     * @return 查询结果
     */
    @PostMapping("/{productId}/event/{eventId}/_query/_format")
    @ResourceAction(id = "query-device-events-format", name = "查询设备事件并返回格式化数据")
    @Operation(summary = "根据产品ID查询事件数据并格式化")
    public Mono<PagerResult<DeviceEvent>> queryDeviceEventsAndFormatValue(
        @PathVariable String productId,
        @PathVariable String eventId,
        @RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(param -> deviceDataService.queryEventPageByProductId(productId, eventId, param, true));
    }

    /**
     * 聚合查询设备属性
     *
     * @param productId 产品ID
     * @return 属性信息
     */
    @PostMapping("/{productId}/agg/{agg}/{property}/_query")
    @ResourceAction(id = "agg-device-property", name = "聚合查询设备属性")
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "根据产品ID聚合查询设备属性")
    public Flux<Map<String, Object>> aggDeviceProperty(@PathVariable @Parameter(description = "产品ID") String productId,
                                                       @PathVariable @Parameter(description = "属性ID") String property,
                                                       @PathVariable @Parameter(description = "聚合类型(count,sum,min,max,avg)") String agg,
                                                       @RequestBody Mono<DeviceDataService.AggregationRequest> param) {

        return param
            .flatMapMany(request -> deviceDataService
                .aggregationPropertiesByProduct(productId,
                                                request,
                                                new DeviceDataService.DevicePropertyAggregation(property, property, Aggregation
                                                    .valueOf(agg.toUpperCase())))
            )
            .map(AggregationData::values);
    }

}
