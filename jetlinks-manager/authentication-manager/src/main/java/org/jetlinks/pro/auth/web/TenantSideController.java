package org.jetlinks.pro.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.*;
import org.jetlinks.pro.auth.entity.TenantMemberEntity;
import org.jetlinks.pro.auth.service.TenantMemberService;
import org.jetlinks.pro.auth.service.request.BindMemberRequest;
import org.jetlinks.pro.auth.service.request.CreateMemberRequest;
import org.jetlinks.pro.auth.service.response.AssetMemberDetail;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.impl.BindAssetsRequest;
import org.jetlinks.pro.tenant.impl.DefaultAssetManager;
import org.jetlinks.pro.tenant.impl.UnbindAssetsRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

/**
 * 租户端,租户管理
 *
 * @author zhouhao
 * @since 1.3
 */
@RestController
@RequestMapping("/tenant")
@Resource(
    id = "tenant-side-manager",
    name = "租户管理-租户端",
    group = "tenant"
)
@Tag(name = "租户管理-租户端", description = "用于管理当前用户所在租户.")
public class TenantSideController {

    private final TenantMemberService memberService;

    private final DefaultAssetManager assetManager;

    public TenantSideController(TenantMemberService memberService,
                                DefaultAssetManager assetManager) {
        this.memberService = memberService;
        this.assetManager = assetManager;
    }

    /**
     * 查询成员信息
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @GetMapping("/members/_query")
    @Authorize(merge = false)
    @QueryOperation(summary = "查询租户成员")
    public Mono<PagerResult<TenantMemberEntity>> getTenantMembers(@Parameter(hidden = true) QueryParamEntity query) {
        return TenantMember
            .current()
            .flatMap(member -> query
                .toQuery()
                .and(TenantMemberEntity::getTenantId, member.getTenant().getId())
                //不是管理员，只能查看自己
                .when(!member.isAdmin(), q -> q.where(TenantMemberEntity::getUserId, member.getUserId()))
                .execute(memberService::queryPager)
            );
    }

    /**
     * 查询成员信息,不返回分页结果
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @GetMapping("/members/_query/no-paging")
    @Authorize(merge = false)
    @QueryOperation(summary = "查询租户成员(不返回总数)")
    public Flux<TenantMemberEntity> getTenantMembersNoPaging(@Parameter(hidden = true) QueryParamEntity query) {
        return TenantMember
            .current()
            .flatMapMany(member -> query
                .toQuery()
                .and(TenantMemberEntity::getTenantId, member.getTenant().getId())
                //不是管理员，只能查看自己
                .when(!member.isAdmin(), q -> q.where(TenantMemberEntity::getUserId, member.getUserId()))
                .execute(memberService::query)
            );
    }

    /**
     * 获取资产下的成员资产信息
     *
     * @return 查询结果
     */
    @GetMapping("/asset/{assetType}/{assetId}/members")
    @QueryAction
    @Operation(summary = "获取某个资产下的成员信息")
    public Flux<AssetMemberDetail> getTenantMembers(@PathVariable @Parameter(description = "资产ID") String assetId,
                                                    @PathVariable @Parameter(description = "资产类型") String assetType,
                                                    @RequestParam(defaultValue = "true")
                                                    @Parameter(description = "是否包含未绑定用户")
                                                        boolean includeUnBind) {

        return TenantMember
            .current()
            .flatMapMany(member -> memberService
                .findAssetMemberDetail(member.getTenant().getId(), assetType, assetId, includeUnBind));
    }

    /**
     * 绑定资产信息
     *
     * @param requestFlux 请求
     * @return 绑定数量
     */
    @PostMapping("/assets/_bind")
    @Authorize(merge = false)
    @Operation(summary = "绑定资产")
    public Mono<Integer> bindAssets(@RequestBody Flux<BindAssetsRequest> requestFlux) {
        return TenantMember
            .current()
            .flatMap(tenant -> assetManager
                .bindAssets(
                    tenant.getTenant().getId(),
                    true,
                    requestFlux)
            );
    }

    /**
     * 解绑资产
     *
     * @param requestFlux 请求
     * @return 解绑数量
     */
    @PostMapping("/assets/_unbind")
    @Authorize(merge = false)
    @Operation(summary = "解绑资产")
    public Mono<Integer> unbindAssets(@RequestBody Flux<UnbindAssetsRequest> requestFlux) {
        return TenantMember
            .current()
            .flatMap(tenant -> assetManager
                .unbindAssets(tenant.getTenant().getId(), requestFlux
                    .doOnNext(request -> {
                        //不是租户管理员,解绑自己的
                        if (!tenant.isAdmin()) {
                            request.setUserId(tenant.getUserId());
                        }
                    })));
    }

    /**
     * 创建成员
     *
     * @param request 请求
     * @return 创建结果
     * @see CreateMemberRequest
     */
    @PostMapping("/member")
    @Authorize(merge = false)
    @Operation(summary = "创建成员")
    public Mono<TenantMemberEntity> createMember(@RequestBody Mono<CreateMemberRequest> request) {
        return Mono.zip(TenantMember.current(), request)
                   .flatMap(tp2 -> memberService.createMember(tp2.getT1().getTenant().getId(), tp2.getT2()));
    }


    /**
     * 设置主租户
     *
     * @param tenantId 请求
     * @return 创建结果
     * @see CreateMemberRequest
     */
    @PutMapping("/{tenantId}/_main")
    @Authorize(merge = false)
    @Operation(summary = "设置为主租户", description = "当一个用户在多个租户中时,可通过此接口切换主租户")
    public Mono<Void> createMember(@PathVariable String tenantId) {
        return TenantMember
            .current()
            .flatMap(member -> memberService.changeMainTenant(tenantId, member.getUserId()));
    }

    /**
     * 绑定成员
     *
     * @param request  请求
     * @return empty Mono
     */
    @PostMapping("/members/_bind")
    @ResourceAction(id = "bind-member",name = "绑定成员")
    @Operation(summary = "绑定成员到指定租户下")
    public Mono<Void> bindMember(@RequestBody Flux<BindMemberRequest> request) {
        return TenantMember
            .current()
            .flatMap(member -> memberService.bindMembers(member.getTenant().getId(), request));
    }

    /**
     * 解绑成员
     *
     * @param request  bindId
     * @return empty Mono
     */
    @PostMapping("/members/_unbind")
    @ResourceAction(id = "unbind-member",name = "解绑成员")
    @Operation(summary = "从指定租户解绑成员")
    public Mono<Void> bindMember(@RequestBody @Parameter(description = "用户ID集合") Mono<List<String>> request) {
        return TenantMember
            .current()
            .flatMap(member ->  memberService.unbindMembers(member.getTenant().getId(), request.flatMapIterable(Function.identity())));
    }

}
