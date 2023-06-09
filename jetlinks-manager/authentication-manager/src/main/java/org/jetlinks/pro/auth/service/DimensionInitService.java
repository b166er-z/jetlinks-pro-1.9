package org.jetlinks.pro.auth.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.system.authorization.api.entity.DimensionTypeEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 维度初始化
 * <p>
 *  实现{@link org.springframework.boot.CommandLineRunner}在项目启动时创建时创建
 *  {@link org.hswebframework.web.system.authorization.api.entity.DimensionTypeEntity}
 *  的机构和角色实例.
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Component
public class DimensionInitService implements CommandLineRunner {

    private final ReactiveRepository<DimensionTypeEntity, String> dimensionTypeRepository;

    public DimensionInitService(ReactiveRepository<DimensionTypeEntity, String> dimensionTypeRepository) {
        this.dimensionTypeRepository = dimensionTypeRepository;
    }

    @Override
    public void run(String... args) {
        DimensionTypeEntity org =new DimensionTypeEntity();
        org.setId("org");
        org.setName("机构");
        org.setDescribe("机构维度");

        DimensionTypeEntity role =new DimensionTypeEntity();
        role.setId("role");
        role.setName("角色");
        role.setDescribe("角色维度");

        dimensionTypeRepository.save(Flux.just(org,role)).subscribe();

    }
}
