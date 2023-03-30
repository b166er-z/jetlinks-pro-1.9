package org.jetlinks.pro.auth.dimension;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.jetlinks.pro.auth.entity.TenantMemberEntity;
import org.jetlinks.pro.auth.enums.TenantMemberState;
import org.jetlinks.pro.auth.enums.TenantState;
import org.jetlinks.pro.auth.service.TenantMemberService;
import org.jetlinks.pro.auth.service.TenantService;
import org.jetlinks.pro.tenant.dimension.TenantDimensionType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 租户维度提供者实现,提供租户维度管理.
 * @author zhouhao
 * @see DimensionProvider
 * @see TenantDimensionType
 * @see TenantMemberService
 * @see TenantService
 * @since 1.3
 */
@Component
@AllArgsConstructor
public class TenantDimensionProvider implements DimensionProvider {

    private final TenantMemberService memberService;

    private final TenantService tenantService;

    /**
     * 获取所有维度类型
     * @see TenantDimensionType
     * @return TenantDimensionType
     */
    @Override
    public Flux<TenantDimensionType> getAllType() {
        return Flux.just(TenantDimensionType.tenantMember, TenantDimensionType.tenantCustomer);
    }

    /**
     * 根据用户id查询维度
     * @param userId
     * @return Dimension
     */
    @Override
    public Flux<Dimension> getDimensionByUserId(String userId) {
        return memberService
            .createQuery()
            .where(TenantMemberEntity::getUserId, userId)
            .and(TenantMemberEntity::getState, TenantMemberState.enabled)
            .fetch()
            .flatMap(this::createDimension)
            ;
    }

    /**
     * 根据维度类型和租户成员id查询维度
     * @see DimensionType
     * @param type
     * @param id
     * @return Dimension
     */
    @Override
    public Mono<? extends Dimension> getDimensionById(DimensionType type, String id) {
        return memberService
            .createQuery()
            .where(TenantMemberEntity::getId, id)
            .and(TenantMemberEntity::getType, type.getId())
            .and(TenantMemberEntity::getState, TenantMemberState.enabled)
            .fetchOne()
            .flatMap(this::createDimension);
    }

    /**
     * 根据维度id查询用户id
     * @param dimensionId
     * @return
     */
    @Override
    @SuppressWarnings("all")
    public Flux<String> getUserIdByDimensionId(String dimensionId) {
        return memberService
            .createQuery()
            .select(TenantMemberEntity::getUserId)
            .where(TenantMemberEntity::getTenantId, dimensionId)
            .fetch()
            .map(TenantMemberEntity::getUserId);

    }

    /**
     * 根据租户成员实体信息创建维度
     * @param member
     * @return
     */
    private Mono<Dimension> createDimension(TenantMemberEntity member) {
        return tenantService //调用租户管理服务查询租户信息
            .findById(member.getTenantId())
            .filter(tenant -> TenantState.enabled.equals(tenant.getState()))
            .map(tenant -> {
                //构建维度基本信息
                SimpleDimension dimension = new SimpleDimension();
                dimension.setId(tenant.getId());
                dimension.setName(tenant.getName());
                dimension.setType(TenantDimensionType.tenant);
                Map<String, Object> opts = new HashMap<>();
                opts.put("memberId", member.getId());
                opts.put("admin", member.getAdminMember());
                opts.put("memberName", member.getName());
                opts.put("memberType", member.getType().name());
                opts.put("main", member.getMainTenant());
                dimension.setOptions(opts);
                return dimension;
            });
    }
}
