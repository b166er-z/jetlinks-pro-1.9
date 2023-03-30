package org.jetlinks.pro.device.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.pro.device.api.v1.response.DeviceInfo;
import org.jetlinks.pro.device.entity.*;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.device.web.response.DeviceDeployResult;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.timeseries.query.Aggregation;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Resource(id = "device-api", name = "设备数据API")
@RequestMapping("/api/v1/device")
@DeviceAsset
@Tag(name = "设备数据API")
public class DeviceDataApiVer1 {

    @Autowired
    private LocalDeviceInstanceService instanceService;

    @Autowired
    private DeviceDataService deviceDataService;

    /**
     * 获取设备详情
     *
     * @param deviceId 设备ID
     * @return 设备详情
     */
    @GetMapping("/{deviceId}/_detail")
    @ResourceAction(id = "get-detail", name = "获取设备详情")
    @Operation(summary = "获取设备详情")
    public Mono<DeviceDetail> getDeviceDetail(@PathVariable String deviceId) {
        return instanceService.getDeviceDetail(deviceId);
    }

    /**
     * 查询设备详情列表
     *
     * @param query 查询条件
     * @return 设备详情
     */
    @PostMapping("/_detail/_query")
    @ResourceAction(id = "get-detail-list", name = "查询设备详情列表")
    @Operation(summary = "动态查询设备详情")
    public Mono<PagerResult<DeviceDetail>> queryDeviceDetail(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .flatMap(instanceService::queryDeviceDetail);
    }

    @PostMapping("/state/_sync")
    @ResourceAction(id = "sync-state", name = "同步设备状态")
    @Operation(summary = "按查询条件同步设备状态")
    public Flux<DeviceStateInfo> syncDeviceState(@RequestBody Mono<QueryParamEntity> query) {
        return query
            .doOnNext(param -> param.noPaging().includes("id"))
            .as(instanceService::query)
            .map(DeviceInstanceEntity::getId)
            .buffer(200)
            .publishOn(Schedulers.single())
            .concatMap(flux -> instanceService.syncStateBatch(Flux.just(flux), true))
            .flatMap(Flux::fromIterable)
            ;
    }

    /**
     * 批量保存设备
     *
     * <pre>
     *
     * POST /api/v1/device
     *
     * [
     *  {
     *     "id":"设备ID",
     *     "name":"设备名称",
     *     "productId":"产品ID",
     *     "configuration":{},//配置信息,根据不同的协议配置不同,如果MQTT用户名密码等.
     *  }
     * ]
     * </pre>
     *
     * @param instanceStream Data
     * @return 保存数量
     */
    @PostMapping
    @ResourceAction(id = "batch-save-device", name = "批量保存设备")
    @TenantAssets(assetObjectIndex = 0)
    @Operation(summary = "批量保存设备")
    public Mono<Integer> saveDevice(@RequestBody Flux<DeviceSaveDetail> instanceStream) {
        return Authentication.currentReactive()
            .flatMapMany(auth -> instanceStream
                .doOnNext(instance -> {
                    instance.setCreatorId(auth.getUser().getId());
                    instance.setCreatorName(auth.getUser().getName());
                }))
            .as(instanceService::batchSave);
    }

    /**
     * 激活设备
     *
     * <pre>
     * POST /api/v1/device/_deploy
     *
     * [
     *  "deviceId1","deviceId2"
     * ]
     *
     * </pre>
     *
     * @param instance id列表
     * @return 激活数量
     */
    @PostMapping("/_deploy")
    @ResourceAction(id = "batch-deploy", name = "批量激活设备")
    @Operation(summary = "批量激活设备")
    public Mono<Integer> batchDeploy(@RequestBody Mono<List<String>> instance) {
        return instance
            .flatMapMany(instanceService::findById)
            .as(instanceService::deploy)
            .map(DeviceDeployResult::getTotal)
            .reduce(Math::addExact);
    }

    /**
     * 注销设备
     *
     * <pre>
     * POST /api/v1/device/_unDeploy
     *
     * [
     *  "deviceId1","deviceId2"
     * ]
     *
     * </pre>
     *
     * @param instance id列表
     * @return 取消激活数量
     */
    @PostMapping("/_unDeploy")
    @ResourceAction(id = "batch-unDeploy", name = "批量注销设备")
    @Operation(summary = "批量注销设备")
    public Mono<Integer> batchUnDeploy(@RequestBody Mono<List<String>> instance) {
        return instance.flatMapMany(Flux::fromIterable)
            .as(instanceService::unregisterDevice);
    }


    /**
     * 删除设备,只会删除状态为未激活的设备.
     *
     * <pre>
     * POST /api/v1/device/_delete
     *
     * [
     *  "deviceId1","deviceId2"
     * ]
     *
     * </pre>
     *
     * @param instance id列表
     * @return 取消激活数量
     */
    @PostMapping("/_delete")
    @ResourceAction(id = "batch-delete", name = "批量删除设备")
    @Operation(summary = "批量删除设备")
    public Mono<Integer> batchDelete(@RequestBody Mono<List<String>> instance) {
        return instance.flatMapMany(Flux::fromIterable)
            .as(instanceService::deleteById);
    }

    /**
     * 查询设备列表
     * <pre>
     *     POST /api/v1/device/_query
     *
     *     {
     *         "pageSize":25,
     *         "pageIndex":0,
     *         "where":"productId is testId"
     *     }
     * </pre>
     *
     * @param param 查询参数
     * @return 查询结果
     */
    @PostMapping("/_query")
    @ResourceAction(id = "get-device-info", name = "查询设备列表")
    @Operation(summary = "查询设备列表")
    public Mono<PagerResult<DeviceInfo>> getDeviceDetail(@RequestBody Mono<QueryParamEntity> param) {
        return param.flatMap(paramEntity -> instanceService
            .queryPager(Mono.just(
                paramEntity.doPaging(
                    paramEntity.getPageIndex(), Math.min(1000, paramEntity.getPageSize())
                )
            ), DeviceInfo::of));
    }


    /**
     * 根据设备ID类型和动态查询参数查询设备相关数据
     *
     * @param deviceId 设备ID
     * @param query    查询参数
     * @return 查询结果
     */
    @PostMapping("/{deviceId}/log/_query")
    @ResourceAction(id = "query-device-log", name = "根据设备ID类型和动态查询参数查询设备相关数据")
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "查询设备日志")
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                      @RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(param -> deviceDataService.queryDeviceMessageLog(deviceId, param));
    }


    /**
     * 获取设备最新的全部属性
     *
     * @param deviceId 设备ID
     * @return 属性信息
     */
    @GetMapping("/{deviceId}/properties/_latest")
    @ResourceAction(id = "query-device-properties-latest", name = "获取设备最新的全部属性")
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "获取设备最新的全部属性")
    public Flux<DeviceProperty> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return deviceDataService.queryEachOneProperties(deviceId, QueryParamEntity.of());
    }

    /**
     * 按查询条件获取设备的全部属性
     *
     * @param deviceId 设备ID
     * @return 属性信息
     */
    @PostMapping("/{deviceId}/properties/_query_each_one")
    @QueryAction
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "获取设备每个属性")
    public Flux<DeviceProperty> queryDeviceEachProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                          @RequestBody Mono<QueryParamEntity> query) {
        return query.flatMapMany(param -> deviceDataService.queryEachOneProperties(deviceId, param));
    }

    //查询属性列表
    @GetMapping("/{deviceId:.+}/properties/_query")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @QueryOperation(summary = "(GET)查询设备指定属性列表")
    @Deprecated
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                   @Parameter(hidden = true) QueryParamEntity entity) {

        return this.doQueryDeviceProperties(deviceId, entity);
    }

    //查询属性列表
    @PostMapping("/{deviceId:.+}/properties/_query")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @Operation(summary = "(POST)查询设备指定属性列表")
    @Deprecated
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                   @Parameter(hidden = true) @RequestBody Mono<QueryParamEntity> queryBody) {
        return queryBody.flatMap(query -> this.doQueryDeviceProperties(deviceId, query));
    }

    private Mono<PagerResult<DeviceProperty>> doQueryDeviceProperties(String deviceId, QueryParamEntity query) {
        return this
            .handlePropertyInQuery(query)
            //参数中指定了property则查询此属性
            .map(property -> deviceDataService.queryPropertyPage(deviceId, property, query))
            .orElseGet(() -> {
                //fixme 查询全部属性,返回的分页结果并不准确
                return deviceDataService
                    .queryEachProperties(deviceId, query)
                    .collectList()
                    .map(dataList -> PagerResult.of(dataList.size(), dataList));
                //没有传property参数
//                return new ValidationException("请设置[property]参数");
            });
    }

    //处理请求中的property
    private Optional<String> handlePropertyInQuery(QueryParamEntity query) {
        List<Term> terms = query.getTerms();
        if (CollectionUtils.isEmpty(terms)) {
            return Optional.empty();
        }
        for (Term term : terms) {
            if ("property".equals(term.getColumn())) {
                String value = String.valueOf(term.getValue());
                term.setValue(null);
                return Optional.ofNullable(value);
            }
        }
        return Optional.empty();
    }

    /**
     * 聚合查询设备属性
     *
     * @param deviceId 设备ID
     * @return 属性信息
     */
    @PostMapping("/{deviceId}/agg/{agg}/{property}/_query")
    @ResourceAction(id = "agg-device-property", name = "聚合查询设备属性")
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "聚合查询设备属性")
    public Flux<Map<String, Object>> aggDeviceProperty(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                       @PathVariable @Parameter(description = "属性ID") String property,
                                                       @PathVariable @Parameter(description = "聚合类型(count,max,min,avg)") String agg,
                                                       @RequestBody Mono<DeviceDataService.AggregationRequest> param) {

        return param
            .flatMapMany(request -> deviceDataService
                .aggregationPropertiesByDevice(deviceId,
                    request,
                    new DeviceDataService.DevicePropertyAggregation(property, property, Aggregation.valueOf(agg.toUpperCase()))))
            .map(AggregationData::values);
    }


    /**
     * 查询设备事件
     * <pre>
     *      POST /api/v1/device/{deviceId}/event/{eventId}/_query
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
     * @param deviceId 设备ID
     * @param eventId  事件ID
     * @param query    查询条件
     * @return 查询结果
     */
    @PostMapping("/{deviceId}/event/{eventId}/_query")
    @ResourceAction(id = "query-device-events", name = "查询设备事件")
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "查询设备事件数据", description = "根据物模型的不同,返回对应的字段,如果物模型定义为Object类型,则对应属性,否则value字段则为事件数据.")
    public Mono<PagerResult<DeviceEvent>> queryDeviceEvents(
        @PathVariable String deviceId,
        @PathVariable String eventId,
        @RequestBody Mono<QueryParamEntity> query) {

        return query
            .flatMap(param -> deviceDataService.queryEventPage(deviceId, eventId, param));
    }

    /**
     * 查询设备事件并返回格式化的结果
     * <pre>
     *      POST /api/v1/device/{deviceId}/event/{eventId}/_query/_format
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
     * @param deviceId 设备ID
     * @param eventId  事件ID
     * @param query    查询条件
     * @return 查询结果
     */
    @PostMapping("/{deviceId}/event/{eventId}/_query/_format")
    @ResourceAction(id = "query-device-events", name = "查询设备事件")
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "查询设备事件格式化数据", description = "查询设备事件数据,并对数据进行格式化.格式化对字段统一为原字段加上_format后缀.")
    public Mono<PagerResult<DeviceEvent>> queryDeviceEventsAndFormat(
        @PathVariable String deviceId,
        @PathVariable String eventId,
        @RequestBody Mono<QueryParamEntity> query) {

        return query
            .flatMap(param -> deviceDataService.queryEventPage(deviceId, eventId, param, true));
    }
}
