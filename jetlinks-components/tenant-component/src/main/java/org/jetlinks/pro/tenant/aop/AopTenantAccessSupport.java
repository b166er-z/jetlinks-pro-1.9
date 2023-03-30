package org.jetlinks.pro.tenant.aop;

import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.annotation.AnnotatedElementUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public class AopTenantAccessSupport extends StaticMethodMatcherPointcutAdvisor {


    @Override
    public boolean matches(@Nonnull Method method,@Nonnull Class<?> aClass) {

        TenantAssets mann = AnnotatedElementUtils.findMergedAnnotation(method, TenantAssets.class);
        TenantAssets cann = AnnotatedElementUtils.findMergedAnnotation(aClass, TenantAssets.class);

        if(mann != null ){
            return !mann.ignore();
        }
        if(cann != null ){
            return !cann.ignore();
        }
        return  false;
    }
}
