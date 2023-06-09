package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.reactor.excel.ReactorExcel;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryNoPagingOperation;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.manager.DeviceBindHolder;
import org.jetlinks.core.device.manager.DeviceBindManager;
import org.jetlinks.core.device.manager.DeviceBindProvider;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.metadata.*;
import org.jetlinks.pro.authorize.OrgDimensionType;
import org.jetlinks.pro.device.entity.*;
import org.jetlinks.pro.device.enums.DeviceState;
import org.jetlinks.pro.device.service.DeviceConfigMetadataManager;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.jetlinks.pro.device.service.LocalDeviceProductService;
import org.jetlinks.pro.device.service.LocalLedInstanceService;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.device.tenant.ProductAsset;
import org.jetlinks.pro.device.web.excel.DeviceExcelInfo;
import org.jetlinks.pro.device.web.excel.DeviceWrapper;
import org.jetlinks.pro.device.web.request.AggRequest;
import org.jetlinks.pro.device.web.response.DeviceDeployResult;
import org.jetlinks.pro.device.web.response.ImportDeviceInstanceResult;
import org.jetlinks.pro.io.excel.ImportExportService;
import org.jetlinks.pro.io.utils.FileUtils;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.util.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/device-instance", "/device/instance"})
@Authorize
@Resource(id = "device-instance", name = "设备实例")
@Slf4j
@DeviceAsset
@Tag(name = "设备实例接口")
public class DeviceInstanceController implements
    TenantAccessCrudController<DeviceInstanceEntity, String> {

    @Getter
    private final LocalDeviceInstanceService service;

    private final LocalDeviceProductService productService;

    private final ImportExportService importExportService;

    private final DeviceRegistry registry;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final DeviceDataService deviceDataService;

    private final DeviceConfigMetadataManager metadataManager;

    private final DeviceBindManager deviceBindManager;

    private final LocalLedInstanceService ledService;

    @SuppressWarnings("all")
    public DeviceInstanceController(LocalDeviceInstanceService service,
                                    DeviceRegistry registry,
                                    ImportExportService importExportService,
                                    LocalDeviceProductService productService,
                                    ReactiveRepository<DeviceTagEntity, String> tagRepository,
                                    DeviceDataService deviceDataService,
                                    DeviceConfigMetadataManager metadataManager,
                                    DeviceBindManager deviceBindManager,
                                    LocalLedInstanceService ledService) {
        this.service = service;
        this.registry = registry;
        this.importExportService = importExportService;
        this.productService = productService;
        this.tagRepository = tagRepository;
        this.deviceDataService = deviceDataService;
        this.metadataManager = metadataManager;
        this.deviceBindManager = deviceBindManager;
        this.ledService = ledService;
    }

    //获取设备详情
    @GetMapping("/{id:.+}/detail")
    @QueryAction
    @DeviceAsset
    @Operation(summary = "获取指定ID设备详情")
    public Mono<DeviceDetail> getDeviceDetailInfo(@PathVariable @Parameter(description = "设备ID") String id) {
        return service.getDeviceDetail(id);
    }

    //获取设备详情
    @GetMapping("/{id:.+}/config-metadata")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "获取设备需要的配置定义信息")
    public Flux<ConfigMetadata> getDeviceConfigMetadata(@PathVariable @Parameter(description = "设备ID") String id) {
        return metadataManager.getDeviceConfigMetadata(id);
    }

    @GetMapping("/{id:.+}/config-metadata/{metadataType}/{metadataId}/{typeId}")
    @QueryAction
    @Operation(summary = "获取设备物模型的拓展配置定义")
    @TenantAssets(ignore = true)
    public Flux<ConfigMetadata> getExpandsConfigMetadata(@PathVariable @Parameter(description = "设备ID") String id,
                                                         @PathVariable @Parameter(description = "物模型类型") DeviceMetadataType metadataType,
                                                         @PathVariable @Parameter(description = "物模型ID") String metadataId,
                                                         @PathVariable @Parameter(description = "类型ID") String typeId) {
        return service
            .findById(id)
            .flatMapMany(device -> metadataManager
                .getMetadataExpandsConfig(device.getProductId(), metadataType, metadataId, typeId, DeviceConfigScope.device));
    }

    @GetMapping("/bind-providers")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "获取支持的云云对接")
    public Flux<DeviceBindProvider> getBindProviders() {
        return Flux.fromIterable(DeviceBindHolder.getAllProvider());
    }

    @DeleteMapping("/{deviceId}/binds/{bindType}/{bindKey}")
    @QueryAction
    @Operation(summary = "解绑云对云接入")
    public Mono<Void> deleteBinds(@PathVariable String deviceId,
                                  @PathVariable String bindType,
                                  @PathVariable String bindKey) {
        return deviceBindManager
            .getBindInfo(bindType, bindKey)
            .filter(bind -> bind.getDeviceId().equals(deviceId))
            .flatMap(bind -> deviceBindManager.unbind(bindType, bindKey))
            .then();
    }

    /**
     * 查询设备详情列表
     *
     * @param query 查询条件
     * @return 设备详情
     */
    @GetMapping("/detail/_query/no-paging")
    @QueryAction
    @QueryNoPagingOperation(summary = "(GET)动态查询设备详情")
    public Flux<DeviceDetail> queryDeviceDetail(@Parameter(hidden = true) QueryParamEntity query) {
        return service.queryDeviceDetailList(query);
    }

    /**
     * 分页查询设备详情列表
     *
     * @param query 查询条件
     * @return 设备详情
     */
    @GetMapping("/detail/_query")
    @QueryAction
    @QueryOperation(summary = "分页动态查询设备详情")
    public Mono<PagerResult<DeviceDetail>> queryDeviceDetailPager(@Parameter(hidden = true) QueryParamEntity query) {
        return service.queryDeviceDetail(query);
    }

    //获取设备运行状态
    @GetMapping("/{id:.+}/state")
    @QueryAction
    @DeviceAsset
    @Operation(summary = "获取指定ID设备在线状态")
    public Mono<DeviceState> getDeviceState(@PathVariable @Parameter(description = "设备ID") String id) {
        return service.getDeviceState(id);
    }

    //激活设备
    @PostMapping("/{deviceId:.+}/deploy")
    @SaveAction
    @DeviceAsset
    @Operation(summary = "激活指定ID设备")
    public Mono<DeviceDeployResult> deviceDeploy(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return service.deploy(deviceId);
    }

    //重置配置信息
    @PutMapping("/{deviceId:.+}/configuration/_reset")
    @SaveAction
    @DeviceAsset
    @Operation(summary = "重置设备配置信息")
    public Mono<Map<String, Object>> resetConfiguration(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return service.resetConfiguration(deviceId);
    }

    //获取设备配置
    @PostMapping("/{deviceId:.+}/configuration/_read")
    @SaveAction
    @DeviceAsset
    @Operation(summary = "获取设备配置信息")
    public Mono<Map<String, Object>> readConfiguration(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                       @RequestBody @Parameter(description = "配置key") Mono<List<String>> configKeys) {
        return Mono
            .zip(
                registry.getDevice(deviceId),
                configKeys,
                DeviceOperator::getConfigs
            )
            .flatMap(Function.identity())
            .map(Values::getAllValues);
    }

    //修改设备配置
    @PostMapping("/{deviceId:.+}/configuration/_write")
    @SaveAction
    @DeviceAsset
    @Operation(summary = "设置设备配置信息")
    public Mono<Void> writeConfiguration(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                         @RequestBody @Parameter(description = "配置信息") Mono<Map<String, Object>> configKeys) {
        return Mono
            .zip(
                registry.getDevice(deviceId),
                configKeys,
                DeviceOperator::setConfigs
            )
            .flatMap(Function.identity())
            .then();
    }

    //批量激活设备
    @GetMapping(value = "/deploy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    @QueryOperation(summary = "查询并批量激活设备")
    public Flux<DeviceDeployResult> deployAll(@Parameter(hidden = true) QueryParamEntity query) {
        query.setPaging(false);
        return service.query(query).as(service::deploy);
    }

    //取消激活
    @PostMapping("/{deviceId:.+}/undeploy")
    @SaveAction
    @DeviceAsset
    @Operation(summary = "注销指定ID的设备")
    public Mono<Integer> unDeploy(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return service.unregisterDevice(deviceId);
    }

    //断开连接
    @PostMapping("/{deviceId:.+}/disconnect")
    @SaveAction
    @Operation(summary = "断开指定ID的设备连接")
    public Mono<Boolean> disconnect(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMapMany(DeviceOperator::disconnect)
            .singleOrEmpty();
    }

    //新建设备
    @PostMapping
    @DeviceAsset(assetObjectIndex = 0, validate = false, autoBind = true)
    @Operation(summary = "新建设备")
    public Mono<DeviceInstanceEntity> add(@RequestBody Mono<DeviceInstanceEntity> payload) {
        return Mono
            .zip(payload, Authentication.currentReactive(), this::applyAuthentication)
            .flatMap(entity -> service.insert(Mono.just(entity))
                .flatMap(m->productService.findById(entity.getProductId()))
                .filter(prod->prod.getClassifiedName().equals("大屏投放终端"))
                .map(p->LedEntity.of(entity))
                .flatMap(ledService :: addNewLed)
                .thenReturn(entity))
            .onErrorMap(DuplicateKeyException.class, err -> new BusinessException("设备ID已存在", err));
    }

    /**
     * 同步设备真实状态
     * @param query 过滤条件
     * @return 实时同步结果
     */
    @GetMapping(value = "/state/_sync", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    @QueryNoPagingOperation(summary = "同步设备状态")
    public Flux<Integer> syncDeviceState(@Parameter(hidden = true) QueryParamEntity query) {
        query.setPaging(false);
        return service
            .query(query.includes("id"))
            .map(DeviceInstanceEntity::getId)
            .buffer(200)
            .publishOn(Schedulers.single())
            .concatMap(flux -> service.syncStateBatch(Flux.just(flux), true).map(List::size))
            .defaultIfEmpty(0);
    }

    //获取设备全部最新属性
    @GetMapping("/{deviceId:.+}/properties/latest")
    @QueryAction
    @DeviceAsset
    @Operation(summary = "获取指定ID设备最新的全部属性")
    public Flux<DeviceProperty> getDeviceLatestProperties(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return deviceDataService.queryEachOneProperties(deviceId, QueryParamEntity.of());
    }

    //获取设备全部属性
    @GetMapping("/{deviceId:.+}/properties")
    @QueryAction
    @DeviceAsset
    @QueryNoPagingOperation(summary = "按条件查询指定ID设备的全部属性")
    public Flux<DeviceProperty> getDeviceProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                    @Parameter(hidden = true) QueryParamEntity queryParamEntity) {
        return deviceDataService.queryEachProperties(deviceId, queryParamEntity);
    }

    //获取设备指定的最新属性
    @GetMapping("/{deviceId:.+}/property/{property:.+}")
    @QueryAction
    @DeviceAsset
    @Operation(summary = "获取指定ID设备最新的属性")
    public Mono<DeviceProperty> getDeviceLatestProperty(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                        @PathVariable @Parameter(description = "属性ID") String property) {
        return deviceDataService.queryEachOneProperties(deviceId, QueryParamEntity.of(), property)
                                .take(1)
                                .singleOrEmpty();
    }

    //根据时间聚合查询前N条数据
    @PostMapping("/{deviceId:.+}/properties/_top/{numberOfTop}")
    @QueryAction
    @DeviceAsset
    @Operation(summary = "根据时间聚合查询前N条数据")
    public Flux<DeviceTopPropertyView> getDeviceTopProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                              @RequestBody Mono<DeviceDataService.AggregationRequest> requestBody,
                                                              @PathVariable int numberOfTop) {

        return requestBody
            .flatMapMany(request -> {
                DateTimeFormatter formatter = DateTimeFormat.forPattern(request.getFormat());

                return deviceDataService
                    .queryTopProperty(deviceId, request.copy(), numberOfTop)
                    .groupBy(DeviceProperty::getFormatTime, Integer.MAX_VALUE)
                    .flatMap(group -> {
                        String time = group.key();
                        return group
                            .filter(prop -> StringUtils.hasText(prop.getProperty()))
                            .groupBy(DeviceProperty::getProperty, Integer.MAX_VALUE)
                            .filter(propertyGroup -> propertyGroup.key() != null)
                            .flatMap(propertyGroup -> {
                                String property = propertyGroup.key();
                                return propertyGroup
                                    .collectList()
                                    .map(list -> DeviceTopProperty.of(property, list));
                            })
                            .collectList()
                            .map(list -> DeviceTopPropertyView.of(time, list));
                    })
                    //按时间排序
                    .sort(Comparator.<DeviceTopPropertyView, DateTime>comparing(pro -> DateTime.parse(pro.time, formatter))
                              .reversed())
                    .take(request.getLimit());
            });
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    public static class DeviceTopPropertyView {
        @Schema(description = "时间")
        private String time;

        @Schema(description = "属性列表")
        private List<DeviceTopProperty> properties;
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    public static class DeviceTopProperty {

        @Schema(description = "属性")
        private String property;

        @Schema(description = "属性数据列表")
        private List<DeviceProperty> data;

    }

    //查询属性列表
    @GetMapping("/{deviceId:.+}/property/{property}/_query")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @QueryOperation(summary = "(GET)查询设备指定属性列表")
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                   @PathVariable @Parameter(description = "属性ID") String property,
                                                                   @Parameter(hidden = true) QueryParamEntity entity) {
        return deviceDataService.queryPropertyPage(deviceId, property, entity);
    }

    //查询属性列表
    @PostMapping("/{deviceId:.+}/property/{property}/_query")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @Operation(summary = "(POST)查询设备指定属性列表")
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                   @PathVariable @Parameter(description = "属性ID") String property,
                                                                   @RequestBody Mono<QueryParamEntity> queryParam) {
        return queryParam.flatMap(param -> deviceDataService.queryPropertyPage(deviceId, property, param));
    }

    //查询属性列表
    @GetMapping("/{deviceId:.+}/properties/_query")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @QueryOperation(summary = "查询设备指定属性列表(已弃用)")
    @Deprecated
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                   @Parameter(hidden = true) QueryParamEntity entity) {
        return entity
            .getTerms()
            .stream()
            .filter(term -> "property".equals(term.getColumn()))
            .findFirst()
            .map(term -> {
                String val = String.valueOf(term.getValue());
                term.setValue(null);
                return val;
            })
            .map(property -> deviceDataService.queryPropertyPage(deviceId, property, entity))
            .orElseThrow(() -> new ValidationException("请设置[property]参数"));

    }

    //查询设备事件数据
    @GetMapping("/{deviceId:.+}/event/{eventId}")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @QueryOperation(summary = "(GET)查询设备事件数据")
    public Mono<PagerResult<DeviceEvent>> queryPagerByDeviceEvent(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                  @PathVariable @Parameter(description = "事件ID") String eventId,
                                                                  @Parameter(hidden = true) QueryParamEntity queryParam,
                                                                  @RequestParam(defaultValue = "false")
                                                                  @Parameter(description = "是否格式化返回结果,格式化对字段添加_format后缀") boolean format) {
        return deviceDataService.queryEventPage(deviceId, eventId, queryParam, format);
    }

    //查询设备事件数据
    @PostMapping("/{deviceId:.+}/event/{eventId}")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @Operation(summary = "(POST)查询设备事件数据")
    public Mono<PagerResult<DeviceEvent>> queryPagerByDeviceEvent(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                  @PathVariable @Parameter(description = "事件ID") String eventId,
                                                                  Mono<QueryParamEntity> queryParam,
                                                                  @RequestParam(defaultValue = "false")
                                                                  @Parameter(description = "是否格式化返回结果,格式化对字段添加_format后缀") boolean format) {
        return queryParam.flatMap(q -> deviceDataService.queryEventPage(deviceId, eventId, q, format));
    }

    //查询设备日志
    @GetMapping("/{deviceId:.+}/logs")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @QueryOperation(summary = "(GET)查询设备日志数据")
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                      @Parameter(hidden = true) QueryParamEntity entity) {
        return deviceDataService.queryDeviceMessageLog(deviceId, entity);
    }

    //查询设备日志
    @PostMapping("/{deviceId:.+}/logs")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @Operation(summary = "(POST)查询设备日志数据")
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                                      @Parameter(hidden = true) Mono<QueryParamEntity> queryParam) {
        return queryParam.flatMap(param -> deviceDataService.queryDeviceMessageLog(deviceId, param));
    }

    //删除标签
    @DeleteMapping("/{deviceId}/tag/{tagId:.+}")
    @SaveAction
    @Operation(summary = "删除设备标签")
    public Mono<Integer> deleteDeviceTag(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                         @PathVariable @Parameter(description = "标签ID") String tagId) {
        return tagRepository.createDelete()
                            .where(DeviceTagEntity::getDeviceId, deviceId)
                            .and(DeviceTagEntity::getId, tagId)
                            .execute();
    }

    /**
     * 批量删除设备,只会删除未激活的设备.
     *
     * @param idList ID列表
     * @return 被删除数量
     * @since 1.1
     */
    @PutMapping("/batch/_delete")
    @DeleteAction
    @Operation(summary = "批量删除未激活的设备")
    public Mono<Integer> deleteBatch(@RequestBody Mono<List<String>> idList) {
        return idList
            .flatMapMany(Flux::fromIterable)
            .as(service::deleteById);
    }

    /**
     * 批量注销设备
     *
     * @param idList ID列表
     * @return 被注销的数量
     * @since 1.1
     */
    @PutMapping("/batch/_unDeploy")
    @SaveAction
    @Operation(summary = "批量注销设备")
    public Mono<Integer> unDeployBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMap(list -> service.unregisterDevice(Flux.fromIterable(list)));
    }

    /**
     * 批量激活设备
     *
     * @param idList ID列表
     * @return 被注销的数量
     * @since 1.1
     */
    @PutMapping("/batch/_deploy")
    @SaveAction
    @Operation(summary = "批量激活设备")
    public Mono<Integer> deployBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMapMany(service::findById)
                     .as(service::deploy)
                     .map(DeviceDeployResult::getTotal)
                     .reduce(Math::addExact);
    }

    /**
     * 获取设备全部标签
     * <pre>
     *     GET /device/instance/{deviceId}/tags
     *
     *     [
     *      {
     *          "id":"id",
     *          "key":"",
     *          "value":"",
     *          "name":""
     *      }
     *     ]
     * </pre>
     *
     * @param deviceId 设备ID
     * @return 设备标签列表
     */
    @GetMapping("/{deviceId}/tags")
    @QueryAction
    @Operation(summary = "获取设备全部标签数据")
    public Flux<DeviceTagEntity> getDeviceTags(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return tagRepository.createQuery()
                            .where(DeviceTagEntity::getDeviceId, deviceId)
                            .fetch();
    }

    //保存设备标签
    @PatchMapping("/{deviceId}/tag")
    @SaveAction
    @Operation(summary = "保存设备标签")
    public Flux<DeviceTagEntity> saveDeviceTag(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                               @RequestBody Flux<DeviceTagEntity> tags) {
        return tags
            .doOnNext(tag -> {
                tag.setId(DeviceTagEntity.createTagId(deviceId, tag.getKey()));
                tag.setDeviceId(deviceId);
                tag.tryValidate();
            })
            .as(tagRepository::save)
            .thenMany(getDeviceTags(deviceId));
    }


    private Mono<Tuple4<DeviceProductEntity, DeviceProductOperator, DeviceMetadata, List<ConfigPropertyMetadata>>> getDeviceProductDetail(String productId) {

        return Mono.zip(
            //T1:产品
            productService.findById(productId),
            //T2:操作接口
            registry.getProduct(productId),
            //T3:物模型
            registry.getProduct(productId).flatMap(DeviceProductOperator::getMetadata),
            //T4:配置定义
            metadataManager.getDeviceConfigMetadataByProductId(productId)
                           .flatMapIterable(ConfigMetadata::getProperties)
                           .collectList()
                           .defaultIfEmpty(Collections.emptyList())
        );
//        return registry
//            .getProduct(productId)
//            .switchIfEmpty(Mono.error(() -> new BusinessException("型号[{" + productId + "]不存在或未发布")))
//            .flatMap(product -> Mono.zip(
//                product.getMetadata(),
//                product.getProtocol(),
//                productService.findById(productId))
//                                    .flatMap(tp3 -> {
//                                        DeviceMetadata metadata = tp3.getT1();
//                                        ProtocolSupport protocol = tp3.getT2();
//                                        DeviceProductEntity entity = tp3.getT3();
//
//                                        return protocol.getSupportedTransport()
//                                                       .collectList()
//                                                       .map(entity::getTransportEnum)
//                                                       .flatMap(Mono::justOrEmpty)
//                                                       .flatMap(protocol::getConfigMetadata)
//                                                       .map(ConfigMetadata::getProperties)
//                                                       .defaultIfEmpty(Collections.emptyList())
//                                                       .map(configs -> Tuples.of(entity, product, metadata, configs));
//
//                                    })
//            );

    }

    //按型号导入数据
    @GetMapping(value = "/{productId}/import", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    @ProductAsset
    @Operation(summary = "导入设备数据")
    public Flux<ImportDeviceInstanceResult> doBatchImportByProduct(@PathVariable @Parameter(description = "产品ID") String productId,
                                                                   @RequestParam(defaultValue = "false") @Parameter(description = "自动启用") boolean autoDeploy,
                                                                   @RequestParam @Parameter(description = "文件地址,支持csv,xlsx文件格式") String fileUrl) {
        return Authentication
            .currentReactive()
            .flatMapMany(auth -> {
                Map<String, String> orgMapping = auth.getDimensions(OrgDimensionType.org)
                                                     .stream()
                                                     .collect(Collectors.toMap(Dimension::getName, Dimension::getId, (_1, _2) -> _1));

                return this
                    .getDeviceProductDetail(productId)
                    .map(tp4 -> Tuples.of(new DeviceWrapper(tp4.getT3().getTags(), tp4.getT4()), tp4.getT1()))
                    .flatMapMany(wrapper ->
                                     importExportService
                                         .getInputStream(fileUrl)
                                         .flatMapMany(inputStream -> ReactorExcel.read(inputStream, FileUtils.getExtension(fileUrl), wrapper
                                             .getT1()))
                                         .doOnNext(info -> info.setProductName(wrapper.getT2().getName()))
                    ).map(info -> {
                        DeviceInstanceEntity entity = FastBeanCopier.copy(info, new DeviceInstanceEntity());
                        entity.setProductId(productId);
                        entity.setOrgId(orgMapping.get(info.getOrgName()));
                        if (StringUtils.isEmpty(entity.getId())) {
                            throw new BusinessException("第" + (info.getRowNumber() + 1) + "行:设备ID不能为空");
                        }
                        return Tuples.of(entity, info.getTags());
                    })
                    .buffer(100)//每100条数据保存一次
                    .publishOn(Schedulers.single())
                    .concatMap(buffer ->
                                   //保存设备以及设备标签信息
                                   Mono.zip(
                                       //判断资产权限
                                       TenantMember
                                           .assertPermission(
                                               Flux.fromIterable(buffer).map(Tuple2::getT1),
                                               DeviceAssetType.device,
                                               DeviceInstanceEntity::getId,
                                               true)
                                           .as(service::save)
                                           .flatMap(res -> {
                                               if (autoDeploy) {
                                                   return service
                                                       .deploy(Flux.fromIterable(buffer).map(Tuple2::getT1))
                                                       .then(Mono.just(res));
                                               }
                                               return Mono.just(res);
                                           }),
                                       //保存标签
                                       Flux.fromIterable(buffer)
                                           .flatMapIterable(Tuple2::getT2)
                                           .as(tagRepository::save)
                                           .defaultIfEmpty(SaveResult.of(0, 0)))
                                       //绑定资产
                                       .flatMap(tp2 -> TenantMember
                                           .bindAssets(
                                               Flux.fromIterable(buffer).map(Tuple2::getT1),
                                               DeviceAssetType.device,
                                               DeviceInstanceEntity::getId)
                                           .then(Mono.just(tp2)))
                    )
                    .map(res -> ImportDeviceInstanceResult.success(res.getT1()))
                    .onErrorResume(err -> Mono.just(ImportDeviceInstanceResult.error(err)));
            });
    }

    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    //获取导入模版
    @GetMapping("/{productId}/template.{format}")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "下载设备导入模版")
    public Mono<Void> downloadExportTemplate(@PathVariable @Parameter(description = "产品ID") String productId,
                                             ServerHttpResponse response,
                                             @PathVariable @Parameter(description = "文件格式,支持csv,xlsx") String format) throws IOException {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                                  "attachment; filename=".concat(URLEncoder.encode("设备导入模版." + format, StandardCharsets.UTF_8
                                      .displayName())));
        return Authentication
            .currentReactive()
            .flatMap(auth -> getDeviceProductDetail(productId)
                .map(tp4 -> DeviceExcelInfo.getTemplateHeaderMapping(tp4.getT3().getTags(), tp4.getT4()))
                .defaultIfEmpty(DeviceExcelInfo.getTemplateHeaderMapping(Collections.emptyList(), Collections.emptyList()))
                .flatMapMany(headers ->
                                 ReactorExcel.<DeviceExcelInfo>writer(format)
                                     .headers(headers)
                                     .converter(DeviceExcelInfo::toMap)
                                     .writeBuffer(Flux.empty()))
                .doOnError(err -> log.error(err.getMessage(), err))
                .map(bufferFactory::wrap)
                .as(response::writeWith));
    }

    //按照产品导出数据.
    @GetMapping("/{productId}/export.{format}")
    @QueryAction
    @ProductAsset(ignoreQuery = true)
    @QueryNoPagingOperation(summary = "按产品导出设备实例数据")
    public Mono<Void> export(@PathVariable @Parameter(description = "产品ID") String productId,
                             ServerHttpResponse response,
                             @Parameter(hidden = true) QueryParamEntity paramEntity,
                             @PathVariable @Parameter(description = "文件格式,支持csv,xlsx") String format) throws IOException {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                                  "attachment; filename=".concat(URLEncoder.encode("设备实例." + format, StandardCharsets.UTF_8
                                      .displayName())));
        return Authentication
            .currentReactive().zipWith(TenantMember.injectQueryParam(paramEntity, DeviceAssetType.device, "id"))
            .flatMap(tp2 -> {
                Authentication auth = tp2.getT1();
                QueryParamEntity parameter = tp2.getT2();
                Map<String, String> orgMapping = auth.getDimensions(OrgDimensionType.org)
                                                     .stream()
                                                     .collect(Collectors.toMap(Dimension::getId, Dimension::getName, (_1, _2) -> _1));
                parameter.setPaging(false);
                parameter.toNestQuery(q -> q.is(DeviceInstanceEntity::getProductId, productId));
                return this
                    .getDeviceProductDetail(productId)
                    .map(tp4 -> Tuples
                        .of(
                            //表头
                            DeviceExcelInfo.getExportHeaderMapping(tp4.getT3().getTags(), tp4.getT4()),
                            //配置key集合
                            tp4
                                .getT4()
                                .stream()
                                .map(ConfigPropertyMetadata::getProperty)
                                .collect(Collectors.toList())
                        ))
                    .defaultIfEmpty(Tuples.of(DeviceExcelInfo.getExportHeaderMapping(Collections.emptyList(),
                                                                                     Collections.emptyList()),
                                              Collections.emptyList()))
                    .flatMapMany(headerAndConfigKey ->
                                     ReactorExcel.<DeviceExcelInfo>writer(format)
                                         .headers(headerAndConfigKey.getT1())
                                         .converter(DeviceExcelInfo::toMap)
                                         .writeBuffer(
                                             service.query(parameter)
                                                    .flatMap(entity -> {
                                                        DeviceExcelInfo exportEntity = FastBeanCopier.copy(entity, new DeviceExcelInfo(), "state");
                                                        exportEntity.setOrgName(orgMapping.get(entity.getOrgId()));
                                                        exportEntity.setState(entity.getState().getText());
                                                        //获取设备真实的配置信息
                                                        return registry
                                                            .getDevice(entity.getId())
                                                            .flatMap(deviceOperator -> deviceOperator
                                                                .getSelfConfigs(headerAndConfigKey.getT2())
                                                                .map(Values::getAllValues))
                                                            .doOnNext(configs -> exportEntity
                                                                .getConfiguration()
                                                                .putAll(configs))
                                                            .thenReturn(exportEntity);
                                                    })
                                                    .buffer(200)
                                                    .flatMap(list -> {
                                                        Map<String, DeviceExcelInfo> importInfo = list
                                                            .stream()
                                                            .collect(Collectors.toMap(DeviceExcelInfo::getId, Function.identity()));
                                                        return tagRepository.createQuery()
                                                                            .where()
                                                                            .in(DeviceTagEntity::getDeviceId, importInfo
                                                                                .keySet())
                                                                            .fetch()
                                                                            .collect(Collectors.groupingBy(DeviceTagEntity::getDeviceId))
                                                                            .flatMapIterable(Map::entrySet)
                                                                            .doOnNext(entry -> importInfo
                                                                                .get(entry.getKey())
                                                                                .setTags(entry.getValue()))
                                                                            .thenMany(Flux.fromIterable(list));
                                                    })
                                             , 512 * 1024))//缓冲512k
                    .doOnError(err -> log.error(err.getMessage(), err))
                    .map(bufferFactory::wrap)
                    .as(response::writeWith);
            });
    }


    //直接导出数据,不支持导出标签.
    @GetMapping("/export.{format}")
    @QueryAction
    @QueryNoPagingOperation(summary = "导出设备实例数据", description = "此操作不支持导出设备标签和配置信息")
    public Mono<Void> export(ServerHttpResponse response,
                             @Parameter(hidden = true) QueryParamEntity parameter,
                             @PathVariable @Parameter(description = "文件格式,支持csv,xlsx") String format) throws IOException {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                                  "attachment; filename=".concat(URLEncoder.encode("设备实例." + format, StandardCharsets.UTF_8
                                      .displayName())));
        parameter.setPaging(false);
        return Authentication
            .currentReactive()
            .flatMap(auth -> {
                Map<String, String> orgMapping = auth.getDimensions(OrgDimensionType.org)
                                                     .stream()
                                                     .collect(Collectors.toMap(Dimension::getId, Dimension::getName, (_1, _2) -> _1));
                return ReactorExcel.<DeviceExcelInfo>writer(format)
                    .headers(DeviceExcelInfo.getExportHeaderMapping(Collections.emptyList(), Collections.emptyList()))
                    .converter(DeviceExcelInfo::toMap)
                    .writeBuffer(
                        service.query(parameter)
                               .map(entity -> {
                                   DeviceExcelInfo exportEntity = FastBeanCopier.copy(entity, new DeviceExcelInfo(), "state");
                                   exportEntity.setOrgName(orgMapping.get(entity.getOrgId()));
                                   exportEntity.setState(entity.getState().getText());
                                   return exportEntity;
                               })
                        , 512 * 1024)//缓冲512k
                    .doOnError(err -> log.error(err.getMessage(), err))
                    .map(bufferFactory::wrap)
                    .as(response::writeWith);
            });
    }

    //设置设备影子
    @PutMapping("/{deviceId:.+}/shadow")
    @SaveAction
    @DeviceAsset
    @Operation(summary = "设置设备影子")
    public Mono<String> setDeviceShadow(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                        @RequestBody Mono<String> shadow) {
        return Mono
            .zip(registry.getDevice(deviceId), shadow)
            .flatMap(tp2 -> tp2.getT1()
                               .setConfig(DeviceConfigKey.shadow, tp2.getT2())
                               .thenReturn(tp2.getT2()));
    }

    //获取设备影子
    @GetMapping("/{deviceId:.+}/shadow")
    @SaveAction
    @DeviceAsset
    @Operation(summary = "获取设备影子")
    public Mono<String> getDeviceShadow(@PathVariable @Parameter(description = "设备ID") String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.shadow))
            .defaultIfEmpty("{\n}");
    }

    //设置设备属性
    @PutMapping("/{deviceId:.+}/property")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "发送设置属性指令到设备", description = "请求示例: {\"属性ID\":\"值\"}")
    public Flux<?> writeProperties(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                   @RequestBody Mono<Map<String, Object>> properties) {
        return properties.flatMapMany(props -> service.writeProperties(deviceId, props));
    }

    //发送设备指令
    @PostMapping("/{deviceId:.+}/message")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "发送指令到设备")
    @SuppressWarnings("all")
    public Flux<?> sendMessage(@PathVariable @Parameter(description = "设备ID") String deviceId,
                               @RequestBody Mono<Map<String, Object>> properties) {
        return properties
            .flatMapMany(props -> {
                return Mono
                    .zip(
                        registry
                            .getDevice(deviceId)
                            .map(DeviceOperator::messageSender)
                            .switchIfEmpty(Mono.error(() -> new NotFoundException("设备不存在或未激活"))),
                        Mono.<Message>justOrEmpty(MessageType.convertMessage(props))
                            .cast(DeviceMessage.class)
                            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的消息格式")))
                    ).flatMapMany(tp2 -> {
                        DeviceMessageSender sender = tp2.getT1();
                        DeviceMessage message = tp2.getT2();

                        Map<String, String> copy = new HashMap<>();
                        copy.put("deviceId", deviceId);
                        if (!StringUtils.hasText(message.getMessageId())) {
                            copy.put("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
                        }
                        FastBeanCopier.copy(copy, message);
                        return sender
                            .send(message)
                            .onErrorResume(DeviceOperationException.class, error -> {
                                if (message instanceof RepayableDeviceMessage) {
                                    return Mono.just(
                                        ((RepayableDeviceMessage) message).newReply().error(error)
                                    );
                                }
                                return Mono.error(error);
                            });
                    });
            });
    }

    //发送设备指令
    @PostMapping("/messages")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "批量发送指令到设备")
    @SuppressWarnings("all")
    @TenantAssets(ignore = true)
    public Flux<?> sendMessage(@RequestParam(required = false)
                               @Parameter(description = "按查询条件发送指令") String where,
                               @RequestBody Flux<Map<String, Object>> messages) {

        Lazy<Flux<DeviceOperator>> operators = Lazy.of(() -> {
            if (StringUtils.isEmpty(where)) {
                throw new ValidationException("where", "[where]参数不能为空");
            }
            QueryParamEntity entity = new QueryParamEntity();
            entity.setWhere(where);
            entity.includes("id");
            //过滤租户数据
            return TenantMember
                .injectQueryParam(entity, DeviceAssetType.device, "id")
                .flatMapMany(service::query)
                .flatMap(device -> registry.getDevice(device.getId()))
                .cache();
        });
        return messages
            .flatMap(message -> {
                DeviceMessage msg = MessageType
                    .convertMessage(message)
                    .filter(DeviceMessage.class::isInstance)
                    .map(DeviceMessage.class::cast)
                    .orElseThrow(() -> new UnsupportedOperationException("不支持的消息格式:" + message));

                String deviceId = msg.getDeviceId();
                Flux<DeviceOperator> devices = StringUtils.isEmpty(deviceId)
                    ? operators.get()
                    : registry.getDevice(deviceId).flux();

                return devices
                    .flatMap(device -> {
                        Map<String, Object> copy = new HashMap<>(message);
                        copy.put("deviceId", device.getDeviceId());
                        copy.putIfAbsent("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
                        //复制为新的消息,防止冲突
                        DeviceMessage copiedMessage = MessageType
                            .convertMessage(copy)
                            .map(DeviceMessage.class::cast)
                            .orElseThrow(() -> new UnsupportedOperationException("不支持的消息格式"));
                        return device
                            .messageSender()
                            .send(copiedMessage)
                            .onErrorResume(Exception.class, error -> {
                                if (copiedMessage instanceof RepayableDeviceMessage) {
                                    return Mono.just(
                                        ((RepayableDeviceMessage) copiedMessage).newReply().error(error)
                                    );
                                }
                                return Mono.error(error);
                            });
                    }, Integer.MAX_VALUE);
            });
    }

    //设备功能调用
    @PostMapping("/{deviceId:.+}/function/{functionId}")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "发送调用设备功能指令到设备", description = "请求示例: {\"参数\":\"值\"}")
    public Flux<?> invokedFunction(@PathVariable String deviceId,
                                   @PathVariable String functionId,
                                   @RequestBody Mono<Map<String, Object>> properties) {

        return properties.flatMapMany(props -> service.invokeFunction(deviceId, functionId, props));
    }

    @PostMapping("/{deviceId:.+}/agg/_query")
    @QueryAction
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "聚合查询设备属性")
    public Flux<Map<String, Object>> aggDeviceProperty(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                                       @RequestBody Mono<AggRequest> param) {

        return param
            .flatMapMany(request -> deviceDataService
                .aggregationPropertiesByDevice(deviceId,
                                               request.getQuery(),
                                               request
                                                   .getColumns()
                                                   .toArray(new DeviceDataService.DevicePropertyAggregation[0]))
            )
            .map(AggregationData::values);
    }


    //更新设备物模型
    @PutMapping(value = "/{id}/metadata")
    @SaveAction
    @Operation(summary = "更新物模型")
    public Mono<Void> updateMetadata(@PathVariable String id,
                                     @RequestBody Mono<String> metadata) {
        return metadata
            .flatMap(metadata_ -> service
                .createUpdate()
                .set(DeviceInstanceEntity::getDeriveMetadata, metadata_)
                .where(DeviceInstanceEntity::getId, id)
                .execute()
                .then(registry.getDevice(id))
                .flatMap(device -> device.updateMetadata(metadata_)))
            .then();
    }


    //合并产品的物模型
    @PutMapping(value = "/{id}/metadata/merge-product")
    @SaveAction
    @Operation(summary = "合并产品的物模型")
    public Mono<Void> mergeProductMetadata(@PathVariable String id) {
        return service
            .findById(id)
            //只有单独保存过物模型的才合并
            .filter(deviceInstance -> StringUtils.hasText(deviceInstance.getDeriveMetadata()))
            .flatMap(deviceInstance -> productService
                .findById(deviceInstance.getProductId())
                .flatMap(product -> deviceInstance.mergeMetadata(product.getMetadata()))
                .then(
                    service
                        .createUpdate()
                        .set(deviceInstance::getDeriveMetadata)
                        .where(deviceInstance::getId)
                        .execute()
                        .then(registry.getDevice(deviceInstance.getId()))
                        .flatMap(device -> device.updateMetadata(deviceInstance.getDeriveMetadata()))
                        .then()
                ));
    }

    //重置设备物模型
    @DeleteMapping(value = "/{id}/metadata")
    @SaveAction
    @Operation(summary = "重置物模型")
    public Mono<Void> resetMetadata(@PathVariable String id) {

        return registry
            .getDevice(id)
            .flatMap(DeviceOperator::resetMetadata)
            .then(service
                      .createUpdate()
                      .setNull(DeviceInstanceEntity::getDeriveMetadata)
                      .where(DeviceInstanceEntity::getId, id)
                      .execute()
                      .then());
    }


}
