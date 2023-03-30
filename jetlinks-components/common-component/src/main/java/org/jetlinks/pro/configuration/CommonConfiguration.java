package org.jetlinks.pro.configuration;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.jetlinks.pro.Interval;
import org.jetlinks.pro.external.JSONataFunctionFeature;
import org.jetlinks.pro.external.SpelFunctionFeature;
import org.jetlinks.pro.external.WindowUntilChangeFeature;
import org.jetlinks.pro.external.WindowUntilFeature;
import org.jetlinks.pro.utils.TimeUtils;
import org.jetlinks.pro.utils.math.MathFlux;
import org.jetlinks.reactor.ql.feature.Feature;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata.addGlobal;

@Configuration
@SuppressWarnings("all")
public class CommonConfiguration {

    public static void load() {
    }

    static {
        MathFlux.load();

        //时间函数
        addGlobal(new FunctionMapFeature("time", 1, 1, arg -> {
            return arg.map(String::valueOf)
                      .map(TimeUtils::parseDate)
                      .map(Date::getTime);
        }));

        addGlobal(new JSONataFunctionFeature());
        addGlobal(new SpelFunctionFeature());
        addGlobal(new WindowUntilChangeFeature());
        addGlobal(new WindowUntilFeature());
        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> aClass, Object o) {
                if (o instanceof String) {
                    o = ((String) o).getBytes();
                }
                if (o instanceof byte[]) {
                    o = Unpooled.wrappedBuffer(((byte[]) o));
                }
                if (o instanceof ByteBuf) {
                    return (T) o;
                }
                return convert(aClass, JSON.toJSONBytes(o));
            }
        }, ByteBuf.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> aClass, Object o) {
                return (T) MediaType.valueOf(String.valueOf(o));
            }
        }, MediaType.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) DataSize.parse(String.valueOf(value));
            }
        }, DataSize.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) TimeUtils.parse(String.valueOf(value));
            }
        }, Duration.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) Interval.of(String.valueOf(value));
            }
        }, Interval.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) TimeUtils.parseUnit(String.valueOf(value));
            }
        }, ChronoUnit.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {

                return (T) ((Long) CastUtils.castNumber(value).longValue());
            }
        }, long.class);

        BeanUtilsBean.getInstance().getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {

                return (T) ((Long) CastUtils.castNumber(value).longValue());
            }
        }, Long.class);
    }

    @Bean
    public ValueMapFeature environmentFeature(Environment environment) {
        return new FunctionMapFeature("env", 2, 1, args -> {
            return args
                .collectList()
                .flatMap(argList -> {
                    return Mono.justOrEmpty(
                        environment.getProperty(String.valueOf(argList.get(0)), argList.size() > 1 ? String.valueOf(argList
                                                                                                                        .get(1)) : null)
                    );
                });
        });
    }

    @Bean
    public BeanPostProcessor globalReactorQlFeatureRegister() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
                if (bean instanceof Feature) {
                    DefaultReactorQLMetadata.addGlobal(((Feature) bean));
                }
                return bean;
            }
        };
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.deserializerByType(Date.class, new SmartDateDeserializer());
        };
    }
}
