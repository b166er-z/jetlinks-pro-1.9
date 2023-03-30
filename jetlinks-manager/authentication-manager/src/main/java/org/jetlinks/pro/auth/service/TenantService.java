package org.jetlinks.pro.auth.service;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.auth.entity.TenantEntity;
import org.jetlinks.pro.auth.entity.TenantMemberDetail;
import org.jetlinks.pro.auth.entity.TenantMemberEntity;
import org.jetlinks.pro.auth.service.request.CreateMemberRequest;
import org.jetlinks.pro.auth.service.request.CreateTenantRequest;
import org.jetlinks.pro.auth.service.response.TenantDetail;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户管理相关操作
 * <p>
 * 通过通用缓存支持的增删改查接口实现租户增删改查功能，并实现相应的缓存能力.
 * 通过实现getCacheName方法指定缓存名称.
 *
 * @author zhouhao
 * @see org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService
 * @since 1.3
 */
@Service
public class TenantService extends GenericReactiveCacheSupportCrudService<TenantEntity, String> {

    private final TenantMemberService memberService;

    public TenantService(TenantMemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public String getCacheName() {
        return "tenants";
    }

    /**
     * 根据租户信息创建租户
     *
     * @param request 创建请求
     * @return 创建后的租户信息
     */
    @Transactional
    public Mono<TenantEntity> createTenant(CreateTenantRequest request) {
        ValidatorUtils.tryValidate(request);

        String id = IDGenerator.MD5.generate();
        TenantEntity entity = request.toTenantEntity();
        entity.setId(id);
        return this
            .insert(Mono.just(entity))//创建租户
            .flatMap(ignore -> memberService
                //创建租户管理员
                .createMember(id, CreateMemberRequest
                    .builder()
                    .admin(true)
                    .name(request.getName())
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .description(request.getDescription())
                    .build()))
            .then(findById(id));

    }

    /**
     * 根据租户id删除租户
     * 可根据多个id进行批量删除
     *
     * @param idPublisher 租户ID
     * @return 删除的租户数量
     */
    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux
            .from(idPublisher)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> memberService
                .createQuery()
                .where()
                .in(TenantMemberEntity::getTenantId, list)
                .count()
                .flatMap(total -> {
                    if (total > 0) {
                        return Mono.error(new UnsupportedOperationException("租户下存在成员信息,无法删除!"));
                    }
                    return super.deleteById(Flux.fromIterable(list));
                }));
    }

    /**
     * 根据通用查询参数分页查询租户详情
     *
     * @param entity 查询条件
     * @return 租户详情信息
     */
    public Mono<PagerResult<TenantDetail>> queryTenantDetail(QueryParamEntity entity) {
        return this
            .queryPager(entity)//分页查询
            .flatMap(result -> Flux
                .fromIterable(result.getData())
                .map(TenantDetail::of)
                .index()
                .flatMap(detail -> memberService
                    .createQuery()//查询租户成员数量
                    .where(TenantMemberEntity::getTenantId, detail.getT2().getTenant().getId())
                    .count()
                    .doOnNext(detail.getT2()::setMembers)
                    .thenReturn(detail))
                .sort(Comparator.comparing(Tuple2::getT1))
                .map(Tuple2::getT2)
                .collectList()
                .map(list -> PagerResult.of(result.getTotal(), list, entity))
            );//返回分页结果
    }
}
