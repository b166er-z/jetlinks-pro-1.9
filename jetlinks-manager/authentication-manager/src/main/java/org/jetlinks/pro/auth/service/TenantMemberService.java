package org.jetlinks.pro.auth.service;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.auth.entity.TenantEntity;
import org.jetlinks.pro.auth.entity.TenantMemberDetail;
import org.jetlinks.pro.auth.entity.TenantMemberEntity;
import org.jetlinks.pro.auth.event.TenantMemberBindEvent;
import org.jetlinks.pro.auth.event.TenantMemberUnBindEvent;
import org.jetlinks.pro.auth.service.request.BindMemberRequest;
import org.jetlinks.pro.auth.service.request.CreateMemberRequest;
import org.jetlinks.pro.auth.service.response.AssetMemberDetail;
import org.jetlinks.pro.tenant.AssetManager;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.dimension.TenantDimensionType;
import org.jetlinks.pro.tenant.impl.UnbindAssetsRequest;
import org.jetlinks.pro.tenant.impl.entity.AssetMemberBindEntity;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 租户成员管理,通过通用接口实现增删改查.
 * <p>
 * 同时提供了绑定解绑成员、创建成员、获取成员关键信息、获取资产信息等功能.
 *
 * @author zhouhao
 * @see GenericReactiveCrudService
 * @since 1.3
 */
@Service
public class TenantMemberService extends GenericReactiveCrudService<TenantMemberEntity, String> {

    private final ReactiveRepository<AssetMemberBindEntity, String> bindRepository;

    private final ReactiveRepository<TenantEntity, String> tenantRepository;

    private final ReactiveUserService userService;

    private final ApplicationEventPublisher eventPublisher;

    private final AssetManager assetManager;

    @SuppressWarnings("all")
    public TenantMemberService(ReactiveRepository<AssetMemberBindEntity, String> bindRepository,
                               ReactiveRepository<TenantEntity, String> tenantRepository,
                               ReactiveUserService userService, ApplicationEventPublisher eventPublisher,
                               AssetManager assetManager) {
        this.assetManager = assetManager;
        this.bindRepository = bindRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.tenantRepository = tenantRepository;
    }

    /**
     * 根据租户id绑定成员
     *
     * @param tenantId 租户ID
     * @param bind     绑定请求
     * @return void
     */
    @Transactional
    public Mono<Void> bindMembers(String tenantId, Flux<BindMemberRequest> bind) {

        Flux<TenantMemberEntity> entityCache = bind
            .map(request -> {
                ValidatorUtils.tryValidate(request);
                TenantMemberEntity entity = request.toMember();
                entity.setTenantId(tenantId);
                entity.setMainTenant(true);
                entity.generateId();
                return entity;
            }).cache();

        return entityCache
            //绑定成功后清除绑定用户的权限设置缓存
            .doOnNext(mem -> eventPublisher.publishEvent(ClearUserAuthorizationCacheEvent.of(mem.getUserId())))
            .as(this::save)
            .then(
                entityCache
                    .map(TenantMemberEntity::getUserId)
                    .collectList()
                    .flatMap(users -> TenantMemberBindEvent
                        .of(tenantId, users)
                        .publish(eventPublisher))
            )
            .then();
    }

    /**
     * 根据租户成员id解绑租户成员
     *
     * @param tenantId     租户ID
     * @param bindIdStream 要解除绑定的成员ID
     * @return void
     */
    @Transactional
    public Mono<Void> unbindMembers(String tenantId, Flux<String> bindIdStream) {

        //启用缓存，此处可能有多个订阅者
        Flux<String> cache = bindIdStream.cache();

        return findById(cache)
            .map(TenantMemberEntity::getUserId)
            .collectList()
            .flatMap(userIdList -> {
                //查询同一个用户下的租户信息,可能存在多个租户
                return this
                    .createQuery()
                    .where()
                    .in(TenantMemberEntity::getUserId, userIdList)
                    .fetch()
                    .groupBy(TenantMemberEntity::getUserId)
                    .flatMap(Flux::collectList)
                    //只处理用户只在一个租户的情况
                    .filter(group -> group.size() == 1)
                    .flatMap(group -> {
                        //租户下的用户类型如果是租户，则禁用此用户
                        return QueryParamEntity
                            .newQuery()
                            .where()
                            .includes(UserEntity::getId)
                            .in(UserEntity::getId, userIdList)
                            .and(UserEntity::getType, TenantDimensionType.tenant.getId())
                            .execute(userService::findUser)
                            .map(UserEntity::getId)
                            .collectList()
                            .filter(CollectionUtils::isNotEmpty)
                            .flatMap(id -> userService.changeState(Flux.fromIterable(id), (byte) 0));
                    })
                    //解绑资产
                    .then(bindRepository
                              .createDelete()
                              .where(AssetMemberBindEntity::getTenantId, tenantId)
                              .in(AssetMemberBindEntity::getUserId, userIdList)
                              .execute()
                    )
                    .then(
                        TenantMemberUnBindEvent.of(tenantId, userIdList).publish(eventPublisher)
                    )
                    //解绑后清除绑定用户的权限设置缓存
                    .then(
                        Mono.fromRunnable(() -> eventPublisher.publishEvent(ClearUserAuthorizationCacheEvent.of(userIdList)))
                    );
            })
            .then(deleteById(cache))
            .then();
    }

    /**
     * 根据租户id和成员信息创建租户成员
     *
     * @param tenantId 租户ID
     * @param request  创建请求
     * @return 创建后的成员信息
     */
    @Transactional
    public Mono<TenantMemberEntity> createMember(String tenantId, CreateMemberRequest request) {
        return userService
            .findByUsername(request.getUsername())//查询用户是否存在
            .map(u -> {
                throw new IllegalArgumentException("用户[" + u.getUsername() + "]已存在");
            })
            .then(Mono.defer(() -> {
                //构建用户信息
                UserEntity user = new UserEntity();
                user.setName(request.getName());
                user.setUsername(request.getUsername());
                user.setCreateTime(System.currentTimeMillis());
                user.setPassword(request.getPassword());
                user.setStatus((byte) 1);
                user.setType(TenantDimensionType.tenant.getId());
                return userService
                    .saveUser(Mono.just(user))
                    .flatMap(ignore -> {
                        TenantMemberEntity entity = request.toMember();
                        entity.setUserId(user.getId());
                        entity.setTenantId(tenantId);
                        entity.generateId();
                        return save(Mono.just(entity))
                            .then(
                                TenantMemberBindEvent
                                    .of(tenantId, Collections.singletonList(user.getId()))
                                    .publish(eventPublisher)
                            )
                            .thenReturn(entity);
                    })
                    ;
            }));
    }

    /**
     * 将指定的成员的主租户设置为指定的租户ID
     *
     * @param tenantId 租户ID
     * @param memberId 成员ID
     * @return
     */
    @Transactional
    public Mono<Void> changeMainTenant(String tenantId, String memberId) {
        return this
            .createUpdate()
            .set(TenantMemberEntity::getMainTenant, false)
            .where(TenantMemberEntity::getUserId, memberId)
            .execute()
            .then(createUpdate()
                      .set(TenantMemberEntity::getMainTenant, true)
                      .where(TenantMemberEntity::getTenantId, tenantId)
                      .and(TenantMemberEntity::getUserId, memberId)
                      .execute())
            //变更成功后清除绑定用户的权限设置缓存
            .doOnNext(mem -> eventPublisher.publishEvent(ClearUserAuthorizationCacheEvent.of(memberId)))
            .then();
    }

    /**
     * 根据用户id获取租户成员信息
     *
     * @param userId 用户ID
     * @return 租户信息
     */
    public Flux<TenantMemberEntity> findByUserId(@Nonnull String userId) {
        Assert.hasText(userId, "userId can not be null");
        return createQuery()
            .where(TenantMemberEntity::getUserId, userId)
            .fetch();
    }

    /**
     * 根据租户id获取租户成员信息
     *
     * @param tenantId 租户ID
     * @return 成员信息
     */
    public Flux<TenantMemberEntity> findByTenantId(@Nonnull String tenantId) {
        Assert.hasText(tenantId, "tenantId can not be null");
        return createQuery()
            .where(TenantMemberEntity::getTenantId, tenantId)
            .fetch();

    }

    /**
     * 根据用户id获取租户成员详情
     *
     * @param userId 用户ID
     * @return 成员详情信息
     */
    public Flux<TenantMemberDetail> findMemberDetail(@Nonnull String userId) {
        Assert.hasText(userId, "userId can not be null");
        return findByUserId(userId)
            .collectList()
            .flatMapMany(members -> {
                Set<String> tenantId = members.stream()
                                              .map(TenantMemberEntity::getTenantId)
                                              .collect(Collectors.toSet());
                return tenantRepository
                    .createQuery()
                    .where().in(TenantEntity::getId, tenantId)
                    .fetch()
                    .collectMap(TenantEntity::getId)
                    .flatMapMany(tenants -> Flux
                        .fromIterable(members)
                        .map(member -> TenantMemberDetail.of(member, tenants.get(member.getTenantId()))));
            });

    }

    /**
     * 查询租户成员资产详情
     *
     * @param tenantId
     * @param assetType
     * @param assetId
     * @param includeUnBind
     * @return
     */
    public Flux<AssetMemberDetail> findAssetMemberDetail(String tenantId,
                                                         String assetType,
                                                         String assetId, boolean includeUnBind) {
        return includeUnBind
            ? findAssetMemberDetailIncludeUnbind(tenantId, assetType, assetId)
            : findAssetMemberDetailOnlyBind(tenantId, assetType, assetId);
    }

    /**
     * 只查询已绑定成员的资产信息
     *
     * @param tenantId
     * @param assetType
     * @param assetId
     * @return
     */
    private Flux<AssetMemberDetail> findAssetMemberDetailOnlyBind(String tenantId,
                                                                  String assetType,
                                                                  String assetId) {

        return Mono
            .zip(
                //资产类型
                assetManager.getAssetType(assetType),
                //绑定关系
                bindRepository
                    .createQuery()
                    .where(AssetMemberBindEntity::getTenantId, tenantId)
                    .and(AssetMemberBindEntity::getAssetType, assetType)
                    .and(AssetMemberBindEntity::getAssetId, assetId)
                    .fetch()
                    .collectList()
                    .filter(CollectionUtils::isNotEmpty)
            )
            .flatMapMany(tp2 -> Flux
                .fromIterable(tp2.getT2())
                .map(bind -> AssetMemberDetail.from(bind, tp2.getT1())))
            .buffer(200)
            .flatMap(buffer -> {
                List<String> userIdList = buffer
                    .stream()
                    .map(AssetMemberDetail::getUserId)
                    .collect(Collectors.toList());
                return this
                    .createQuery()
                    .where()
                    //批量查询成员信息,然后和结果进行关联
                    .in(TenantMemberEntity::getUserId, userIdList)
                    .fetch()
                    .collectMap(TenantMemberEntity::getUserId, Function.identity())
                    .flatMapMany(members -> Flux
                        .fromIterable(buffer)
                        .doOnNext(mem -> Optional.ofNullable(members.get(mem.getUserId())).ifPresent(mem::with)));
            });
    }

    /**
     * 获取包含未绑定成员的租户成员资产信息
     *
     * @param tenantId
     * @param assetType
     * @param assetId
     * @return
     */
    private Flux<AssetMemberDetail> findAssetMemberDetailIncludeUnbind(String tenantId,
                                                                       String assetType,
                                                                       String assetId) {
        return Mono
            .zip(
                this.findByTenantId(tenantId).collectList(),
                bindRepository
                    .createQuery()
                    .where(AssetMemberBindEntity::getTenantId, tenantId)
                    .and(AssetMemberBindEntity::getAssetType, assetType)
                    .and(AssetMemberBindEntity::getAssetId, assetId)
                    .fetch()
                    .collectMap(AssetMemberBindEntity::getUserId, Function.identity()),
                assetManager.getAssetType(assetType))
            .flatMapMany(tp3 -> {
                List<TenantMemberEntity> memberMapping = tp3.getT1();
                Map<String, AssetMemberBindEntity> bindMapping = tp3.getT2();
                AssetType type = tp3.getT3();
                return Flux.fromIterable(memberMapping)
                           .map(mem -> AssetMemberDetail.from(mem, bindMapping.get(mem.getUserId()), type, assetId));
            });
    }

}
