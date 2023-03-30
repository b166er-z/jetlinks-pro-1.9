package org.jetlinks.pro.reactorql;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ReactorQLFactoryBean implements FactoryBean<Object>, InitializingBean, Ordered {

    @Getter
    @Setter
    private Class<?> target;

    private Object proxy;

    private static final ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    public ReactorQLFactoryBean() {

    }

    @Override
    public Object getObject() {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return target;
    }

    @Override
    public void afterPropertiesSet() {
        Map<Method, Function<Object[], Publisher<?>>> cache = new ConcurrentHashMap<>();
        ReflectionUtils.doWithMethods(target, method -> {
            ReactorQL ql = method.getAnnotation(ReactorQL.class);
            if (ql == null && !method.isDefault()) {
                throw new UnsupportedOperationException("请在方法[" + method + "]上注解ReactorQL或者使用default关键字修饰");
            }
        });
        this.proxy = Proxy.newProxyInstance(ClassUtils.getDefaultClassLoader(), new Class[]{target}, (proxy, method, args) -> {
            if (method.isDefault()) {
                return method.invoke(proxy, args);
            }
            return cache
                .computeIfAbsent(method, mtd -> createInvoker(target, mtd, mtd.getAnnotation(ReactorQL.class)))
                .apply(args);
        });
    }


    private Function<Object[], Publisher<?>> createInvoker(Class<?> type, Method method, ReactorQL ql) {
        ResolvableType returnType = ResolvableType.forMethodReturnType(method);
        if (returnType.toClass() != Mono.class && returnType.toClass() != Flux.class) {
            throw new UnsupportedOperationException("方法返回值必须为Mono或者Flux");
        }
        Class<?> genericType = returnType.getGeneric(0).toClass();
        Function<Map<String, Object>, ?> mapper;

        if (genericType == Map.class || genericType == Object.class) {
            mapper = Function.identity();
        } else {
            mapper = map -> FastBeanCopier.copy(map, genericType);
        }

        Function<Flux<?>, Publisher<?>> resultMapper =
            returnType.resolve() == Mono.class
                ? flux -> flux.take(1).singleOrEmpty()
                : flux -> flux;

        String[] names = nameDiscoverer.getParameterNames(method);

        try {
            org.jetlinks.reactor.ql.ReactorQL reactorQL =
                org.jetlinks.reactor.ql.ReactorQL
                    .builder()
                    .sql(ql.value())
                    .build();

            return args -> {
                Map<String, Object> argsMap = new HashMap<>();
                ReactorQLContext context = ReactorQLContext.ofDatasource(name -> {
                    if (args.length == 0) {
                        return Flux.just(1);
                    }
                    if (args.length == 1) {
                        return convertToFlux(args[0]);
                    }
                    return convertToFlux(argsMap.get(name));
                });
                for (int i = 0; i < args.length; i++) {
                    String indexName = "arg" + i;

                    String name = names == null ? indexName : names[i];
                    context.bind(i, args[i]);
                    context.bind(name, args[i]);
                    context.bind(indexName, args[i]);
                    argsMap.put(names == null ? indexName : names[i], args[i]);
                    argsMap.put(indexName, args[i]);
                }
                return reactorQL.start(context)
                                .map(record -> mapper.apply(record.asMap()))
                                .as(resultMapper);
            };
        } catch (Throwable e) {
            throw new IllegalArgumentException(
                "create ReactorQL method [" + method + "] error,sql:\n" + (String.join(" ", ql.value())), e);
        }
    }

    private Flux<Object> convertToFlux(Object arg) {
        if (arg == null) {
            return Flux.empty();
        }
        if (arg instanceof Publisher) {
            return Flux.from((Publisher<?>) arg);
        }
        if (arg instanceof Iterable) {
            return Flux.fromIterable(((Iterable<?>) arg));
        }
        if (arg instanceof Object[]) {
            return Flux.fromArray(((Object[]) arg));
        }
        return Flux.just(arg);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
