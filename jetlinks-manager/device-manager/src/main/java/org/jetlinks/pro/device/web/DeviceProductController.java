package org.jetlinks.pro.device.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceConfigScope;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.jetlinks.pro.device.entity.DeviceLatestData;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.jetlinks.pro.device.entity.DeviceProperty;
import org.jetlinks.pro.device.service.DeviceConfigMetadataManager;
import org.jetlinks.pro.device.service.LocalDeviceProductService;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.device.service.data.DeviceDataStoragePolicy;
import org.jetlinks.pro.device.service.data.DeviceLatestDataService;
import org.jetlinks.pro.device.tenant.DeviceAsset;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.device.tenant.ProductAsset;
import org.jetlinks.pro.device.web.request.AggRequest;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.tenant.crud.TenantAccessCrudController;
import org.jetlinks.pro.timeseries.query.AggregationData;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/device-product", "/device/product"})
@Resource(id = "device-product", name = "设备产品")
@Authorize
@ProductAsset
@Tag(name = "产品接口")
public class DeviceProductController implements TenantAccessCrudController<DeviceProductEntity, String> {

    private final LocalDeviceProductService productService;

    private final List<DeviceDataStoragePolicy> policies;

    private final DeviceDataService deviceDataService;

    private final DeviceLatestDataService latestDataService;

    private final DeviceConfigMetadataManager configMetadataManager;

    private final ObjectProvider<DeviceMetadataCodec> metadataCodecs;

    private final DeviceMetadataCodec defaultCodec = new JetLinksDeviceMetadataCodec();

    public DeviceProductController(DeviceLatestDataService latestDataService,
                                   LocalDeviceProductService productService,
                                   List<DeviceDataStoragePolicy> policies,
                                   DeviceDataService deviceDataService,
                                   DeviceConfigMetadataManager configMetadataManager,
                                   ObjectProvider<DeviceMetadataCodec> metadataCodecs) {
        this.latestDataService = latestDataService;
        this.productService = productService;
        this.policies = policies;
        this.deviceDataService = deviceDataService;
        this.configMetadataManager = configMetadataManager;
        this.metadataCodecs = metadataCodecs;
    }

    @Override
    public LocalDeviceProductService getService() {
        return productService;
    }

    @GetMapping("/{id:.+}/config-metadata")
    @QueryAction
    @Operation(summary = "获取产品需要的配置定义信息")
    @TenantAssets(ignore = true)
    public Flux<ConfigMetadata> getDeviceConfigMetadata(@PathVariable
                                                        @Parameter(description = "产品ID") String id) {
        return configMetadataManager.getProductConfigMetadata(id);
    }

    @GetMapping("/{id:.+}/config-metadata/{metadataType}/{metadataId}/{typeId}")
    @QueryAction
    @Operation(summary = "获取产品物模型的拓展配置定义")
    @TenantAssets(ignore = true)
    public Flux<ConfigMetadata> getExpandsConfigMetadata(@PathVariable @Parameter(description = "产品ID") String id,
                                                         @PathVariable @Parameter(description = "物模型类型") DeviceMetadataType metadataType,
                                                         @PathVariable @Parameter(description = "物模型ID") String metadataId,
                                                         @PathVariable @Parameter(description = "类型ID") String typeId) {
        return configMetadataManager
            .getMetadataExpandsConfig(id, metadataType, metadataId, typeId, DeviceConfigScope.product);
    }

    @GetMapping("/metadata/codecs")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "获取支持的物模型格式")
    public Flux<DeviceMetadataCodec> getMetadataCodec() {
        return Flux.fromIterable(metadataCodecs);
    }

    @PostMapping("/metadata/convert-to/{id}")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "转换平台的物模型为指定的物模型格式")
    public Mono<JSONObject> convertMetadataTo(@RequestBody Mono<String> metadata,
                                              @PathVariable String id) {

        return metadata
            .flatMap(str -> Flux
                .fromIterable(metadataCodecs)
                .filter(codec -> codec.getId().equals(id))
                .next()
                .flatMap(codec -> defaultCodec
                    .decode(str)
                    .flatMap(codec::encode))
                .map(JSON::parseObject));
    }

    @PostMapping("/metadata/convert-from/{id}")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "转换指定的物模型为平台的物模型格式")
    public Mono<JSONObject> convertMetadataFrom(@RequestBody Mono<String> metadata,
                                                @PathVariable String id) {

        return metadata
            .flatMap(str -> Flux
                .fromIterable(metadataCodecs)
                .filter(codec -> codec.getId().equals(id))
                .next()
                .flatMap(codec -> codec
                    .decode(str)
                    .flatMap(defaultCodec::encode))
                .map(JSON::parseObject));
    }


    @PostMapping("/{productId:.+}/deploy")
    @SaveAction
    @Operation(summary = "激活产品")
    public Mono<Integer> deviceDeploy(@PathVariable
                                      @Parameter(description = "产品ID") String productId) {
        return productService.deploy(productId);
    }

    @PostMapping("/{productId:.+}/undeploy")
    @SaveAction
    @Operation(summary = "注销产品")
    public Mono<Integer> cancelDeploy(@PathVariable @Parameter(description = "产品ID") String productId) {
        return productService.cancelDeploy(productId);
    }

    @GetMapping("/storage/policies")
    @Operation(summary = "获取支持的数据存储策略")
    public Flux<DeviceDataStorePolicyInfo> storePolicy() {
        return Flux.fromIterable(policies)
                   .flatMap(DeviceDataStorePolicyInfo::of);
    }

    //查询属性列表
    @GetMapping("/{productId:.+}/property/{property}/_query")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @QueryOperation(summary = "(GET)查询设备指定属性列表")
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String productId,
                                                                   @PathVariable @Parameter(description = "属性ID") String property,
                                                                   @Parameter(hidden = true) QueryParamEntity entity) {
        return deviceDataService.queryPropertyPageByProductId(productId, property, entity);
    }

    //查询属性列表
    @PostMapping("/{productId:.+}/property/{property}/_query")
    @QueryAction
    @DeviceAsset(ignoreQuery = true)
    @Operation(summary = "(POST)查询设备指定属性列表")
    public Mono<PagerResult<DeviceProperty>> queryDeviceProperties(@PathVariable @Parameter(description = "设备ID") String productId,
                                                                   @PathVariable @Parameter(description = "属性ID") String property,
                                                                   @RequestBody Mono<QueryParamEntity> queryParam) {
        return queryParam.flatMap(param -> deviceDataService.queryPropertyPageByProductId(productId, property, param));
    }

    @PostMapping("/{productId:.+}/agg/_query")
    @QueryAction
    @TenantAssets(ignoreQuery = true)
    @Operation(summary = "聚合查询产品下设备属性")
    public Flux<Map<String, Object>> aggDeviceProperty(@PathVariable
                                                       @Parameter(description = "产品ID") String productId,
                                                       @RequestBody Mono<AggRequest> param) {

        return param
            .flatMapMany(request -> deviceDataService
                .aggregationPropertiesByProduct(productId,
                                                request.getQuery(),
                                                request
                                                    .getColumns()
                                                    .toArray(new DeviceDataService.DevicePropertyAggregation[0]))
            )
            .map(AggregationData::values);
    }

    @PostMapping("/{productId:.+}/latest/_query")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "(POST)查询产品下设备最新数据")
    public Mono<PagerResult<DeviceLatestData>> getProductLatestData(@PathVariable
                                                                    @Parameter(description = "产品ID") String productId,
                                                                    @RequestBody Mono<QueryParamEntity> param) {

        return param
            .flatMap(request -> TenantMember
                .injectQueryParam(request, DeviceAssetType.device, "id")
                .flatMap(query -> latestDataService.queryPager(productId, query)));
    }

    @GetMapping("/{productId:.+}/latest/_query")
    @QueryAction
    @TenantAssets(ignore = true)
    @QueryOperation(summary = "(GET)查询产品下设备最新数据")
    public Mono<PagerResult<DeviceLatestData>> getProductLatestData(@PathVariable
                                                                    @Parameter(description = "产品ID") String productId,
                                                                    @Parameter(hidden = true) QueryParamEntity param) {

        return TenantMember
            .injectQueryParam(param, DeviceAssetType.device, "id")
            .flatMap(query -> latestDataService.queryPager(productId, query));
    }

    @PostMapping("/{productId:.+}/latest/agg/_query")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(summary = "聚合查询产品下设备最新数据")
    public Mono<Map<String, Object>> getProductLatestAggData(@PathVariable
                                                             @Parameter(description = "产品ID") String productId,
                                                             @RequestBody Mono<DeviceLatestDataService.QueryLatestDataRequest> param) {

        return param
            .flatMap(request -> TenantMember
                .injectQueryParam(request.getQuery(), DeviceAssetType.device, "id")
                .flatMap(query -> latestDataService.aggregation(productId, request.getColumns(), query)));
    }

    @PostMapping("/_multi/latest/agg/_query")
    @QueryAction
    @TenantAssets(ignore = true)
    @Operation(
        summary = "聚合查询多个产品下设备最新数据",
        description = "如果指定了合并结果,将根据第一个查询条件中对应列的聚合类型,对结果进行合并计算.")
    public Flux<Map<String, Object>> getMultiProductLatestAggData(@RequestBody Flux<DeviceLatestDataService.QueryProductLatestDataRequest> requests,
                                                                  @RequestParam(defaultValue = "false")
                                                                  @Parameter(description = "是否合并结果(默认false)")
                                                                      boolean merge) {


        return requests
            .flatMap(request -> TenantMember
                .injectQueryParam(request.getQuery(), DeviceAssetType.device, "id")
                .doOnNext(request::setQuery)
                .thenReturn(request))
            .as(param -> latestDataService.aggregation(param, merge));
    }

    @PostMapping("/{productId}/metadata/merge-to-device")
    @SaveAction
    @Operation(summary = "合并物模型到产品下的所有设备")
    public Mono<Void> mergeMetadataToDevice(@PathVariable @Parameter(description = "产品ID") String productId) {
        return productService.mergeMetadataToDevice(productId);
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceDataStorePolicyInfo {
        @Schema(description = "策略ID")
        private String id;

        @Schema(description = "策略名称")
        private String name;

        @Schema(description = "说明")
        private String description;

        @Schema(description = "配置定义")
        private ConfigMetadata configMetadata;

        public static Mono<DeviceDataStorePolicyInfo> of(DeviceDataStoragePolicy policy) {
            return policy.getConfigMetadata()
                         .map(metadata -> new DeviceDataStorePolicyInfo(policy.getId(), policy.getName(), policy.getDescription(), metadata))
                         .defaultIfEmpty(new DeviceDataStorePolicyInfo(policy.getId(), policy.getName(), policy.getDescription(), null));
        }
    }
}

