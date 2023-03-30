package org.jetlinks.pro.tenant.aop;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.annotation.Phased;
import org.jetlinks.pro.tenant.annotation.TenantAssets;
import org.jetlinks.pro.tenant.term.MultiAssetsTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class AopTenantAccessHandler implements MethodInterceptor {

    private static final Map<CacheKey, Handler> handlers = new ConcurrentHashMap<>();

    private final TransactionalOperator transactionalOperator;

    @Override
    @SneakyThrows
    public Object invoke(MethodInvocation methodInvocation) {

        Method method = methodInvocation.getMethod();
        Class<?> target = ClassUtils.getUserClass(methodInvocation.getThis());

        Handler handler = handlers.computeIfAbsent(new CacheKey(method, target), this::createHandler);

        if (handler.getPhased() == Phased.before || handler.getPhased() == Phased.both) {
            handler.handle(methodInvocation.getArguments(), null, Phased.before);
        }
        Object result = methodInvocation.proceed();
        if (handler.getPhased() == Phased.after || handler.getPhased() == Phased.both) {
            result = handler.handle(methodInvocation.getArguments(), result, Phased.after);
        }
        //控制事务
        if(handler.transactional()){
            if(result instanceof Mono){
                return transactionalOperator.transactional(((Mono<?>) result));
            }
            if(result instanceof Flux){
                return transactionalOperator.transactional(((Flux<?>) result));
            }
        }
        return result;
    }

    public Handler createHandler(CacheKey key) {
        Method method = key.method;
        Class<?> type = key.target;

        TenantAssets methodAnn = AnnotatedElementUtils.findMergedAnnotation(method, TenantAssets.class);
        TenantAssets typeAnn = AnnotatedElementUtils.findMergedAnnotation(type, TenantAssets.class);
        AssetsDefine define = AssetsDefine.of(typeAnn, methodAnn);
        if (StringUtils.isEmpty(define.type)) {
            throw new UnsupportedOperationException("没有指定资产类型:" + type);
        }
        if (methodAnn == null && typeAnn == null) {
            return Handler.DO_NOTING;
        }
        return createHandler(method, define);
    }

    private static class AssetsDefine {
        private String type;
        private int assetIdIndex = -1;
        private int assetObjectIndex = -1;
        private String property = "id";
        private boolean required = true;
        private boolean autoBind = false;
        private boolean autoUnBind = false;
        private boolean ignoreQuery = false;
        private boolean validate = true;
        private boolean allowAssetNotExist = false;

        private void apply(TenantAssets ann) {
            if (StringUtils.hasText(ann.type())) {
                type = ann.type();
            }
            if (ann.assetIdIndex() != -1) {
                assetIdIndex = ann.assetIdIndex();
            }
            if (ann.assetObjectIndex() != -1) {
                assetObjectIndex = ann.assetObjectIndex();
            }
            if (ann.ignoreQuery()) {
                ignoreQuery = true;
            }
            if (ann.autoBind()) {
                autoBind = true;
            }
            if (ann.autoUnbind()) {
                autoUnBind = true;
            }
            if (!ann.validate()) {
                validate = false;
            }
            if (ann.allowAssetNotExist()) {
                allowAssetNotExist = ann.allowAssetNotExist();
            }
            required = ann.required();
            if (!"id".equals(ann.property())) {
                property = ann.property();
            }

        }

        private static AssetsDefine of(TenantAssets typeAnn, TenantAssets methodAnn) {
            AssetsDefine define = new AssetsDefine();

            if (typeAnn != null) {
                define.apply(typeAnn);
            }
            if (methodAnn != null) {
                define.apply(methodAnn);
            }

            return define;
        }
    }

    public Handler createHandler(Method method, AssetsDefine ann) {

        int index = 0;
        boolean returnMono = method.getReturnType().isAssignableFrom(Mono.class);
        boolean returnFlux = method.getReturnType().isAssignableFrom(Flux.class);
        if (!returnMono && !returnFlux) {
            log.warn("方法不支持响应式:{}", method);
            return Handler.DO_NOTING;
        }
        if (method.getParameterCount() == 0) {
            return Handler.DO_NOTING;
        }

        //查询参数
        if (!ann.ignoreQuery) {
            for (Parameter parameter : method.getParameters()) {
                ResolvableType type = ResolvableType.forMethodParameter(method, index);
                Class<?> argType = type.toClass();

                if (QueryParamEntity.class.isAssignableFrom(argType)) {
                    return new QueryParamHandler(index, ann, returnMono, returnFlux);
                }
                if (Mono.class.isAssignableFrom(argType)) {
                    Class<?> gen = type.getGeneric(0).toClass();
                    if (QueryParamEntity.class.isAssignableFrom(gen)) {
                        return new MonoQueryParamHandler(index, ann, returnMono, returnFlux);
                    }
                }
                index++;
            }
        }

        //按对象过滤权限
        if (ann.assetObjectIndex != -1) {
            index = ann.assetObjectIndex;
            ResolvableType type = ResolvableType.forMethodParameter(method, index);

            if (Mono.class.isAssignableFrom(type.toClass())) {
                if (ann.autoBind) {
                    return new MonoAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          obj -> getProperty(obj, ann.property),
                                                          (tenant, objects) -> tenant.bindAssets(ann.type, objects)
                    );
                } else if (ann.autoUnBind) {
                    return new MonoAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          obj -> getProperty(obj, ann.property),
                                                          (tenant, objects) -> tenant.unbindAssets(ann.type, objects)
                    );
                }
                return new MonoHandler(index, ann);
            }
            if (Flux.class.isAssignableFrom(type.toClass())) {
                if (ann.autoBind) {
                    return new FluxAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          list -> getProperties(list, ann.property),
                                                          (tenant, objects) -> tenant.bindAssets(ann.type, objects)
                    );
                } else if (ann.autoUnBind) {
                    return new FluxAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          list -> getProperties(list, ann.property),
                                                          (tenant, objects) -> tenant.unbindAssets(ann.type, objects)
                    );
                }
                return new FluxHandler(index, ann);
            }
            if (ann.autoUnBind) {
                return new AutoBindHandler(index, ann, returnMono, returnFlux,
                                           list -> getProperties(list, ann.property),
                                           (tenant, objects) -> tenant.unbindAssets(ann.type, objects)
                );
            } else if (ann.autoBind) {
                return new AutoBindHandler(index, ann, returnMono, returnFlux,
                                           list -> getProperties(list, ann.property),
                                           (tenant, objects) -> tenant.bindAssets(ann.type, objects)
                );
            }
            return new AssetsHandler(index, ann, returnMono, returnFlux,
                                     obj -> getProperties((obj), ann.property));
        }

        //按id过滤权限
        if (ann.assetIdIndex != -1) {
            index = ann.assetIdIndex;
            ResolvableType type = ResolvableType.forMethodParameter(method, index);
            index = ann.assetIdIndex;
            if (Mono.class.isAssignableFrom(type.toClass())) {
                if (ann.autoBind) { //绑定
                    return new MonoAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          Optional::of,
                                                          (tenant, objects) -> tenant.bindAssets(ann.type, objects)
                    );
                } else if (ann.autoUnBind) { //解绑
                    return new MonoAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          Optional::of,
                                                          (tenant, objects) -> tenant.unbindAssets(ann.type, objects)
                    );
                }
                return new MonoIdHandler(index, ann);
            }
            if (Flux.class.isAssignableFrom(type.toClass())) {
                if (ann.autoBind) { //绑定
                    return new FluxAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          list -> list,
                                                          (tenant, objects) -> tenant.bindAssets(ann.type, objects)
                    );
                } else if (ann.autoUnBind) { //解绑
                    return new FluxAutoBindByAssetHandler(index, ann, returnMono, returnFlux,
                                                          list -> list,
                                                          (tenant, objects) -> tenant.unbindAssets(ann.type, objects)
                    );
                }
                return new FluxIdHandler(index, ann);
            }
            if (ann.autoUnBind) {
                return new AutoBindHandler(index, ann, returnMono, returnFlux,
                                           list -> list,
                                           (tenant, objects) -> tenant.unbindAssets(ann.type, objects)
                );
            } else if (ann.autoBind) {
                return new AutoBindHandler(index, ann, returnMono, returnFlux,
                                           list -> list,
                                           (tenant, objects) -> tenant.bindAssets(ann.type, objects)
                );
            }
            return new AssetsHandler(index, ann, returnMono, returnFlux, Function.identity());
        }

        return Handler.DO_NOTING;
    }

    private static Mono<TenantMember> getCurrentTenant(boolean required) {
        return required
            ? TenantMember.current().switchIfEmpty(Mono.error(AccessDenyException::new))
            : TenantMember.current();
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    static class CacheKey {

        Method method;
        Class<?> target;

    }

    interface Handler {

        Handler DO_NOTING = new Handler() {
            @Override
            public Phased getPhased() {
                return Phased.before;
            }

            @Override
            public Object handle(Object[] args, Object result, Phased phased) {
                return result;
            }
        };

        Phased getPhased();

        Object handle(Object[] args, Object result, Phased phased);

       default boolean transactional(){
           return false;
       }
    }

    public static Optional<Object> getProperty(Object obj, String prop) {
        Map<String, Object> property = FastBeanCopier.copy(obj, new HashMap<>(), FastBeanCopier.include(prop));
        return Optional.ofNullable(property.get(prop));
    }

    public static List<Object> getProperties(Collection<?> obj, String prop) {
        return obj
            .stream()
            .map(v -> getProperty(v, prop).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @AllArgsConstructor
    static class FluxAutoBindByAssetHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;
        private final boolean returnMono;
        private final boolean returnFlux;

        private final Function<List<?>, Collection<?>> argMapper;

        private final BiFunction<TenantMember, Collection<?>, Mono<?>> binAction;


        @Override
        public Phased getPhased() {
            return Phased.both;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {

            if (phased == Phased.before) {
                Flux<?> tmp = ((Flux<?>) args[argIndex]).cache();

                if (ann.validate) {

                    @SuppressWarnings("all")
                    Flux<?> arg = Mono.zip(TenantMember.current(), tmp.collectList())
                        .<Object>flatMapMany(tp2 -> tp2.getT1()
                                                       .assetPermission(ann.type, argMapper.apply(tp2.getT2()), ann.allowAssetNotExist)
                                                       .thenMany(Flux.fromIterable(tp2.getT2())))
                        .switchIfEmpty(tmp);

                    args[argIndex] = arg;
                } else {
                    args[argIndex] = tmp;
                }

                return result;
            } else {
                if (returnMono) {
                    @SuppressWarnings("all")
                    Mono<Object> mono = ((Mono<Object>) result).cache();
                    return Mono
                        .zip(TenantMember.current(), ((Flux<?>) args[argIndex]).collectList())
                        .flatMap(tp2 -> mono
                            .flatMap(r -> binAction
                                .apply(tp2.getT1(), argMapper.apply(tp2.getT2()))
                                .thenReturn(r))
                            .switchIfEmpty(Mono.defer(() ->
                                                          mono.then(binAction
                                                                        .apply(tp2.getT1(), argMapper.apply(tp2.getT2()))
                                                                        .then(Mono.empty()))))
                        ).switchIfEmpty(mono);
                } else {
                    @SuppressWarnings("all")
                    Flux<Object> flux = ((Flux<Object>) result).cache();
                    return Mono.zip(TenantMember.current(), ((Flux<?>) args[argIndex]).collectList())
                               .flatMapMany(tp2 -> flux
                                   .flatMap(r -> binAction
                                       .apply(tp2.getT1(), argMapper.apply(tp2.getT2()))
                                       .thenReturn(r))
                                   .switchIfEmpty(Flux.defer(() ->
                                                                 flux
                                                                     .then(binAction.apply(tp2.getT1(), argMapper.apply(tp2.getT2())))
                                                                     .then(Mono.empty())
                                   )))
                               .switchIfEmpty(flux);
                }

            }

        }

        @Override
        public boolean transactional() {
            return true;
        }
    }

    @AllArgsConstructor
    static class MonoAutoBindByAssetHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;
        private final boolean returnMono;
        private final boolean returnFlux;

        private final Function<Object, Optional<?>> argMapper;

        private final BiFunction<TenantMember, Collection<?>, Mono<?>> binAction;

        @Override
        public Phased getPhased() {
            return Phased.both;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {

            if (phased == Phased.before) {
                Mono<?> tmp = ((Mono<?>) args[argIndex]).cache();

                if (ann.validate) {

                    //修改参数
                    args[argIndex] = Mono.zip(TenantMember.current(), tmp)
                        .<Object>flatMap(tp2 -> {
                            Object arg = argMapper.apply(tp2.getT2()).orElse(null);
                            if (arg == null) {
                                return Mono.just(tp2.getT2());
                            }
                            return tp2.getT1()
                                      .assetPermission(ann.type, Collections.singleton(arg), ann.allowAssetNotExist)
                                      .thenReturn(tp2.getT2());
                        })
                        .switchIfEmpty(tmp);
                } else {
                    args[argIndex] = tmp;
                }

                return result;
            } else {
                if (returnMono) {
                    @SuppressWarnings("all")
                    Mono<Object> mono = ((Mono<Object>) result).cache();
                    return Mono
                        .zip(TenantMember.current(), ((Mono<?>) args[argIndex]))
                        .flatMap(tp2 -> mono
                            .flatMap(r -> binAction
                                .apply(
                                    tp2.getT1(),
                                    Collections.singleton(argMapper
                                                              .apply(tp2.getT2())
                                                              .orElseThrow(() -> new IllegalArgumentException("can not get id property")))
                                )
                                .thenReturn(r))
                            .switchIfEmpty(Mono.defer(() -> mono.then(binAction
                                                                          .apply(
                                                                              tp2.getT1(),
                                                                              Collections.singleton(argMapper
                                                                                                        .apply(tp2.getT2())
                                                                                                        .orElseThrow(() -> new IllegalArgumentException("can not get id property")))
                                                                          )
                                                                          .then(Mono.empty()))))
                        ).switchIfEmpty(mono);
                } else {
                    @SuppressWarnings("all")
                    Flux<Object> flux = ((Flux<Object>) result).cache();
                    return Mono
                        .zip(TenantMember.current(), ((Mono<?>) args[argIndex]))
                        .flatMapMany(tp2 -> flux
                            .flatMap(r -> binAction
                                .apply(tp2.getT1(), Collections.singleton(argMapper.apply(tp2.getT2())))
                                .thenReturn(r))
                            .switchIfEmpty(Mono.defer(() -> flux.then(binAction
                                                                          .apply(tp2.getT1(), Collections.singleton(argMapper
                                                                                                                        .apply(tp2.getT2())))
                                                                          .then(Mono.empty()))))
                        ).switchIfEmpty(flux);
                }

            }

        }
        @Override
        public boolean transactional() {
            return true;
        }
    }

    @AllArgsConstructor
    static class AutoBindHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;
        private final boolean returnMono;
        private final boolean returnFlux;

        private final Function<Collection<?>, Collection<?>> idMapper;

        private final BiFunction<TenantMember, Collection<?>, Mono<Void>> operator;


        @Override
        public Phased getPhased() {
            return Phased.after;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {

            Collection<?> id = idMapper.apply(
                args[argIndex] instanceof Collection
                    ? ((Collection<?>) args[argIndex])
                    : Collections.singleton(args[argIndex]));

            return TenantMember.current()
                               .flatMap(tenant -> ann.validate
                                   ? tenant.assetPermission(ann.type, id, ann.allowAssetNotExist).thenReturn(tenant)
                                   : Mono.just(tenant))
                               .flatMap(tenant -> operator.apply(tenant, id))
                               .as(v -> returnMono ? v.then(((Mono<?>) result)) : v.thenMany(((Flux<?>) result)));

        }

        @Override
        public boolean transactional() {
            return true;
        }
    }

    @AllArgsConstructor
    static class MonoHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;

        @Override
        public Phased getPhased() {
            return Phased.before;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {
            Mono<?> mono = ((Mono<?>) args[argIndex]).cache();

            args[argIndex] = getCurrentTenant(ann.required)
                .zipWith(mono)
                .<Object>flatMap(tp2 -> {
                    Map<String, Object> property = FastBeanCopier.copy(tp2.getT2(), new HashMap<>(), FastBeanCopier.include(ann.property));
                    Object prop = property.get(ann.property);
                    if (prop == null) {
                        return Mono.just(tp2.getT2());
                    }
                    return tp2.getT1()
                              .assetPermission(ann.type,
                                               prop instanceof Collection
                                                   ? ((Collection<String>) prop) :
                                                   Collections.singletonList(String.valueOf(prop))
                                  , ann.allowAssetNotExist)
                              .thenReturn(tp2.getT2());

                })
                .switchIfEmpty(mono);

            return null;
        }
    }

    @AllArgsConstructor
    static class FluxHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;

        @Override
        public Phased getPhased() {
            return Phased.before;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {

            Flux<?> flux = ((Flux<?>) args[argIndex]).cache();

            args[argIndex] = getCurrentTenant(ann.required)
                .zipWith(flux.collectList())
                .<Object>flatMapMany(tp2 -> {
                    List<String> idList = tp2.getT2()
                                             .stream()
                                             .map(objs -> {
                                                 Map<String, Object> property = FastBeanCopier.copy(tp2.getT2(), new HashMap<>(), FastBeanCopier
                                                     .include(ann.property));
                                                 return property.get(ann.property);
                                             })
                                             .filter(Objects::nonNull)
                                             .map(String::valueOf)
                                             .collect(Collectors.toList());

                    return tp2.getT1()
                              .assetPermission(ann.type, idList, ann.allowAssetNotExist)
                              .thenMany(Flux.fromIterable(tp2.getT2()));
                })
                .switchIfEmpty(flux);

            return null;
        }
    }

    @AllArgsConstructor
    static class AssetsHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;

        private final boolean returnMono;
        private final boolean returnFlux;

        private final Function<Collection<?>, Collection<?>> idMapper;

        @Override
        public Phased getPhased() {
            return Phased.after;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {

            Collection<?> id = idMapper.apply(
                args[argIndex] instanceof Collection
                    ? ((Collection<?>) args[argIndex])
                    : Collections.singleton(args[argIndex]));

            Mono<?> returnVal = getCurrentTenant(ann.required)
                .zipWith(Mono.just(id))
                .flatMap(tp2 -> tp2.getT1().assetPermission(ann.type, tp2.getT2(), ann.allowAssetNotExist));

            if (returnMono) {
                return returnVal.then(((Mono<?>) result));
            }
            if (returnFlux) {
                return returnVal.thenMany(((Flux<?>) result));
            }
            return result;
        }
    }

    @AllArgsConstructor
    static class FluxIdHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;

        @Override
        public Phased getPhased() {
            return Phased.before;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {

            Flux<?> flux = ((Flux<?>) args[argIndex]).cache();

            args[argIndex] = getCurrentTenant(ann.required)
                .zipWith(flux.collectList())
                .<Object>flatMapMany(tp2 ->
                                         tp2.getT1()
                                            .assetPermission(ann.type, tp2.getT2(), ann.allowAssetNotExist)
                                            .thenMany(Flux.fromIterable(tp2.getT2()))
                )
                .switchIfEmpty(flux);

            return result;
        }
    }

    @AllArgsConstructor
    static class MonoIdHandler implements Handler {
        private final int argIndex;

        private final AssetsDefine ann;

        @Override
        public Phased getPhased() {
            return Phased.before;
        }

        @Override
        @SuppressWarnings("all")
        public Object handle(Object[] args, Object result, Phased phased) {

            Mono<?> mono = ((Mono<?>) args[argIndex]).cache();

            args[argIndex] = getCurrentTenant(ann.required)
                .zipWith(mono)
                .<Object>flatMap(tp2 -> tp2.getT1()
                                           .assetPermission(ann.type
                                               , tp2.getT2() instanceof Collection
                                                                ? ((Collection) tp2.getT2())
                                                                : Collections.singletonList(String.valueOf(tp2.getT2()))
                                               , ann.allowAssetNotExist)
                                           .thenReturn(tp2.getT2())
                )
                .switchIfEmpty(mono);

            return result;
        }
    }

    @AllArgsConstructor
    static class MonoQueryParamHandler implements Handler {

        private final int argIndex;

        private final AssetsDefine ann;

        private final boolean returnMono;
        private final boolean returnFlux;


        @Override
        public Phased getPhased() {
            return Phased.after;
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {
            @SuppressWarnings("all")
            Mono<QueryParamEntity> param = ((Mono<QueryParamEntity>) args[argIndex]).cache();


            args[argIndex] = param;

            Mono<?> mono = getCurrentTenant(ann.required)
                .zipWith(param)
                .doOnNext(tp2 -> QueryParamHandler.doHandle(tp2.getT1(), ann, tp2.getT2()));

            if (returnMono) {
                return mono.then(((Mono<?>) result));
            } else if (returnFlux) {
                return mono.thenMany(((Flux<?>) result));
            } else {
                return result;
            }
        }
    }

    @AllArgsConstructor
    static class QueryParamHandler implements Handler {

        private final int argIndex;

        private final AssetsDefine ann;

        private final boolean returnMono;
        private final boolean returnFlux;

        @Override
        public Phased getPhased() {
            return Phased.after;
        }

        static void doHandle(TenantMember tenant, AssetsDefine ann, QueryParamEntity param) {
            //注入查询条件

            param.toNestQuery(query ->
                                  query.and(
                                      ann.property,
                                      MultiAssetsTerm.ID,
                                      MultiAssetsTerm.from(ann.type, tenant)));
        }

        @Override
        public Object handle(Object[] args, Object result, Phased phased) {
            QueryParamEntity param = (QueryParamEntity) args[argIndex];

            Mono<?> mono = getCurrentTenant(ann.required)
                .doOnNext(tenant -> doHandle(tenant, ann, param));

            if (returnMono) {
                return mono.then(((Mono<?>) result));
            } else if (returnFlux) {
                return mono.thenMany(((Flux<?>) result));
            } else {
                return result;
            }
        }
    }


}
