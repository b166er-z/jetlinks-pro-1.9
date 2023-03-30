package org.jetlinks.pro.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.auth.entity.UserDetail;
import org.jetlinks.pro.auth.entity.UserDetailEntity;
import org.jetlinks.pro.auth.service.request.SaveUserDetailRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 用户详情管理
 * <p>
 * 通过通用增删改查接口实现用户详情增删改查功能.
 * 通过用户id获取用户基本信息（包含租户成员信息）
 * @author zhouhao
 * @see org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService
 * @since 1.3
 */
@Service
@AllArgsConstructor
public class UserDetailService extends GenericReactiveCrudService<UserDetailEntity, String> {

    private final ReactiveUserService userService;

    private final TenantMemberService memberService;

    private final static UserDetailEntity emptyDetail = new UserDetailEntity();

    /**
     * 根据用户id获取用户详情
     * @param userId
     * @return
     */
    public Mono<UserDetail> findUserDetail(String userId) {
        return Mono
            .zip(
                userService.findById(userId), // 基本信息
                this.findById(userId).defaultIfEmpty(emptyDetail), // 详情
                memberService.findMemberDetail(userId).collectList() // 租户成员信息
            )
            .map(tp4 -> UserDetail.of(tp4.getT1())
                .with(tp4.getT2())
                .with(tp4.getT3()));
    }

    /**
     * 根据用户id和用户信息保存用户详情
     * @param userId
     * @param request
     * @return
     */
    public Mono<Void> saveUserDetail(String userId, SaveUserDetailRequest request) {
        ValidatorUtils.tryValidate(request);
        UserDetailEntity entity = FastBeanCopier.copy(request, new UserDetailEntity());
        entity.setId(userId);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setName(request.getName());

        return save(Mono.just(entity))
            .then(userService.saveUser(Mono.just(userEntity)))
            .then();
    }

}
