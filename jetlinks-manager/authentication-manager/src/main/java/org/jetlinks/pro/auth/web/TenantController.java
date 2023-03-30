package org.jetlinks.pro.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceDeleteController;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceSaveController;
import org.jetlinks.pro.auth.entity.TenantEntity;
import org.jetlinks.pro.auth.entity.TenantMemberEntity;
import org.jetlinks.pro.auth.enums.TenantState;
import org.jetlinks.pro.auth.service.TenantMemberService;
import org.jetlinks.pro.auth.service.TenantService;
import org.jetlinks.pro.auth.service.request.*;
import org.jetlinks.pro.auth.service.response.AssetMemberDetail;
import org.jetlinks.pro.auth.service.response.TenantDetail;
import org.jetlinks.pro.tenant.impl.BindAssetsRequest;
import org.jetlinks.pro.tenant.impl.DefaultAssetManager;
import org.jetlinks.pro.tenant.impl.UnbindAssetsRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

/**
 * 管理端,租户管理
 *
 * @author zhouhao
 * @since 1.3
 */
@RestController
@RequestMapping("/tenant")
@Resource(id = "tenant-manager", name = "租户管理-管理端")
@Tag(name = "租户管理-管理端", description = "用于管理任意租户,进行相关操作.")
public class TenantController implements
    ReactiveServiceQueryController<TenantEntity, String>,
    ReactiveServiceSaveController<TenantEntity, String>,
    ReactiveServiceDeleteController<TenantEntity, String> {

    private final TenantService tenantService;

    private final TenantMemberService memberService;

    private final DefaultAssetManager assetManager;

    public TenantController(TenantService tenantService,
                            TenantMemberService memberService,
                            DefaultAssetManager assetManager) {
        this.tenantService = tenantService;
        this.memberService = memberService;
        this.assetManager = assetManager;
    }

    @Override
    public ReactiveCrudService<TenantEntity, String> getService() {
        return tenantService;
    }

    /**
     * 创建租户
     *
     * @param requestMono 请求
     * @return 不报错就成功
     */
    @PostMapping("/_create")
    @SaveAction
    @Operation(summary = "创建租户")
    public Mono<TenantEntity> getTenantMembers(@RequestBody Mono<CreateTenantRequest> requestMono) {
        return requestMono.flatMap(tenantService::createTenant);
    }

    /**
     * 分页查询租户详细列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @GetMapping("/detail/_query")
    @QueryAction
    @QueryOperation(summary = "查询租户详情")
    public Mono<PagerResult<TenantDetail>> queryTenantDetail(@Parameter(hidden = true) QueryParamEntity query) {
        return tenantService.queryTenantDetail(query);
    }

    /**
     * 查询成员信息
     *
     * @param tenantId 租户ID
     * @param query    查询条件
     * @return 查询结果
     */
    @GetMapping("/{tenantId}/members/_query")
    @QueryAction
    @QueryOperation(summary = "查询租户成员")
    public Mono<PagerResult<TenantMemberEntity>> getTenantMembers(@PathVariable String tenantId,
                                                                  @Parameter(hidden = true) QueryParamEntity query) {
        return query.toQuery()
                    .and(TenantMemberEntity::getTenantId, tenantId)
                    .execute(memberService::queryPager);
    }

    /**
     * 查询成员信息,不返回分页结果
     *
     * @param query    查询条件
     * @param tenantId 租户ID
     * @return 查询结果
     */
    @GetMapping("/{tenantId}/members/_query/no-paging")
    @QueryAction
    @QueryOperation(summary = "查询租户成员(不返回总数)")
    public Flux<TenantMemberEntity> getTenantMembersNoPaging(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                                             @Parameter(hidden = true) QueryParamEntity query) {
        return query
            .toQuery()
            .and(TenantMemberEntity::getTenantId, tenantId)
            .execute(memberService::query);
    }

    /**
     * 获取资产下的成员信息
     *
     * @param tenantId  租户ID
     * @param assetId   资产ID
     * @param assetType 资产类型
     * @return 查询结果
     */
    @GetMapping("/{tenantId}/asset/{assetType}/{assetId}/members")
    @QueryAction
    @Operation(summary = "获取某个资产下的成员信息")
    public Flux<AssetMemberDetail> getTenantMembers(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                                    @PathVariable @Parameter(description = "资产类型") String assetType,
                                                    @PathVariable @Parameter(description = "资产ID") String assetId,
                                                    @RequestParam(defaultValue = "true")
                                                    @Parameter(description = "是否包含未绑定用户")
                                                        boolean includeUnBind) {

        return memberService.findAssetMemberDetail(tenantId, assetType, assetId, includeUnBind);
    }

    /**
     * 绑定指定租户下的资产
     *
     * @param tenantId    租户ID
     * @param requestFlux 请求
     * @return 绑定数量
     */
    @PostMapping("/{tenantId}/assets/_bind")
    @SaveAction
    @Operation(summary = "绑定资产到指定到租户下")
    public Mono<Integer> bindAssets(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                    @RequestBody Flux<BindAssetsRequest> requestFlux) {
        return assetManager.bindAssets(tenantId, false, requestFlux);
    }

    /**
     * 修改租户状态
     *
     * @param tenantId 租户ID
     * @param state    状态
     * @return 成功1 失败0
     */
    @PutMapping("/{tenantId}/state/_{state}")
    @SaveAction
    @Operation(summary = "修改租户状态")
    public Mono<Integer> saveState(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                   @PathVariable @Parameter(description = "状态") TenantState state) {
        return tenantService
            .createUpdate()
            .set(TenantEntity::getState, state)
            .where(TenantEntity::getId, tenantId)
            .execute();
    }

    /**
     * 解绑指定租户下的资产
     *
     * @param tenantId    租户ID
     * @param requestFlux 请求
     * @return 解绑数量
     */
    @PostMapping("/{tenantId}/assets/_unbind")
    @SaveAction
    @Operation(summary = "解绑租户资产")
    public Mono<Integer> unbindAssets(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                      @RequestBody Flux<UnbindAssetsRequest> requestFlux) {
        return assetManager.unbindAssets(tenantId, requestFlux);
    }


    /**
     * 给指定租户创建成员信息
     *
     * @param tenantId 租户ID
     * @param request  请求
     * @return 结果
     */
    @PostMapping("/{tenantId}/member")
    @SaveAction
    @Operation(summary = "给指定租户创建成员")
    public Mono<TenantMemberEntity> createMember(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                                 @RequestBody Mono<CreateMemberRequest> request) {
        return request.flatMap(createRequest -> memberService.createMember(tenantId, createRequest));
    }

    /**
     * 绑定成员
     *
     * @param tenantId 租户ID
     * @param request  请求
     * @return empty Mono
     */
    @PostMapping("/{tenantId}/members/_bind")
    @SaveAction
    @Operation(summary = "绑定成员到指定租户下")
    public Mono<Void> bindMember(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                 @RequestBody Flux<BindMemberRequest> request) {
        return memberService.bindMembers(tenantId, request);
    }

    /**
     * 解绑成员
     *
     * @param tenantId 租户ID
     * @param request  bindId
     * @return empty Mono
     */
    @PostMapping("/{tenantId}/members/_unbind")
    @SaveAction
    @Operation(summary = "从指定租户解绑成员")
    public Mono<Void> bindMember(@PathVariable @Parameter(description = "租户ID") String tenantId,
                                 @RequestBody @Parameter(description = "用户ID集合") Mono<List<String>> request) {
        return memberService.unbindMembers(tenantId, request.flatMapIterable(Function.identity()));
    }


}
