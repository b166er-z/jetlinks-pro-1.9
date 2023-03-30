package org.jetlinks.pro.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.jetlinks.pro.auth.web.request.AuthorizationSettingDetail;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
/**
 * 授权设置管理,用于保存授权配置以及获取授权设置详情.
 *
 * @author zhouhao
 * @see DefaultAuthorizationSettingService,DimensionProvider
 * @since 1.0
 */
@Component
@AllArgsConstructor
public class AuthorizationSettingDetailService {

    private final DefaultAuthorizationSettingService settingService;
    private final List<DimensionProvider> providers;

    /**
     * 保存授权设置详情
     * @param detailFlux
     * @return
     */
    @Transactional
    public Mono<Void> saveDetail(Flux<AuthorizationSettingDetail> detailFlux) {
        return detailFlux
            //先删除旧的权限设置
            .flatMap(detail -> settingService.getRepository().createDelete()
                .where(AuthorizationSettingEntity::getDimensionType, detail.getTargetType())
                .and(AuthorizationSettingEntity::getDimensionTarget, detail.getTargetId())
                .execute()
                .thenReturn(detail))
            .flatMap(detail ->
                Flux.fromIterable(providers)
                    .flatMap(provider -> provider
                        .getAllType()
                        .filter(type -> type.getId().equals(detail.getTargetType()))//过滤掉不同的维度类型
                        .singleOrEmpty()
                        .flatMap(type -> provider.getDimensionById(type, detail.getTargetId()))
                        .flatMapIterable(detail::toEntity))
                    .switchIfEmpty(Flux.defer(() -> Flux.fromIterable(detail.toEntity())))
                    .distinct(AuthorizationSettingEntity::getPermission)
            )
            .as(settingService::save)
            .then();
    }

    //获取权限详情
    public Mono<AuthorizationSettingDetail> getSettingDetail(String targetType,
                                                             String target) {
        return settingService
            .createQuery()
            .where(AuthorizationSettingEntity::getDimensionTarget, target)
            .and(AuthorizationSettingEntity::getDimensionType, targetType)
            .fetch()
            .collectList()
            .map(AuthorizationSettingDetail::fromEntity)
            ;
    }

}
