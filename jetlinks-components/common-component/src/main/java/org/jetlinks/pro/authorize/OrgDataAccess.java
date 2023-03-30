package org.jetlinks.pro.authorize;

import org.hswebframework.web.authorization.annotation.DimensionDataAccess;
import org.hswebframework.web.authorization.define.Phased;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 在Controller或者方法上注解,用于按组织机构控制数据权限
 *
 * @author wangzheng
 * @author zhouhao
 * @see OrgDataAccessCrudController
 * @see org.hswebframework.web.authorization.Authentication
 * @see org.hswebframework.web.authorization.DimensionType
 * @see org.hswebframework.web.authorization.simple.DimensionDataAccessConfig
 * @since 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@DimensionDataAccess
@DimensionDataAccess.Mapping(dimensionType = "org", property = "orgId")
@Deprecated
public @interface OrgDataAccess {

    /**
     * 验证的权限的阶段，如果为{@link Phased#before},则在执行方法逻辑之前进行判断,此时会根据不同的参数类型进行不同的处理.
     * <p>
     * 如果为{@link Phased#after}时,则在方法执行后进行判断,将根据返回结果来进行不同的处理.
     *
     * @return 验证阶段
     */
    @AliasFor(annotation = DimensionDataAccess.class)
    Phased phased() default Phased.before;

    /**
     * 获取ID的参数索引,如果值大于等于0,在进行相关操作的时候,将使用参数列表中对应索引的参数作为ID,去查询对应的数据来判断权限.
     *
     * @return 参数索引
     */
    @AliasFor(annotation = DimensionDataAccess.Mapping.class)
    int idParamIndex() default 0;
}
