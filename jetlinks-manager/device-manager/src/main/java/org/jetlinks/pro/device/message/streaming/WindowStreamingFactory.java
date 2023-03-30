package org.jetlinks.pro.device.message.streaming;

import com.alibaba.fastjson.JSON;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.pro.streaming.Computer;
import org.jetlinks.pro.streaming.Streaming;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

@Getter
@Setter
@EqualsAndHashCode
public class WindowStreamingFactory {

    private WindowType windowType = WindowType.time;

    private Window window;

    private String script;

    private String property;

    private AggType aggType = AggType.count;

    public void validate() {
        Assert.notNull(windowType, "[windowType]不能为空");
        Assert.notNull(window, "[window]不能为空");
        window.validate();
        Assert.hasText(script, "[script]不能为空");
        Assert.notNull(aggType, "[aggType]不能为空");

    }

    public Streaming<DeviceMessage, Object, DeviceMessage> create() {
        validate();
        Computer<DeviceMessage, Object> computer = ScriptDevicePropertyStreaming.create(script);

        Function<Flux<Object>, Flux<Flux<Object>>> windowFunction
            = (windowType == WindowType.time ? window::windowTime : window::window);
        //使用规则配置的md5值作为规则的唯一标示,避免重复重启相同的规则
        String jsonConfig = JSON.toJSONString(this);
        String identity = DigestUtils.md5Hex(jsonConfig);

        return DefaultStreaming
            .create(identity,
                    property, computer, flux -> windowFunction
                    .apply(flux)
                    .flatMap(window -> aggType
                        .getComputer()
                        .apply(window), Integer.MAX_VALUE));
    }

    public enum WindowType {
        time,
        num
    }

    @Getter
    @Setter
    public static class Window {
        private int span;
        private int every;

        public void validate() {
            Assert.state(span > 0, "[span]不能小于等于0");
        }

        public Flux<Flux<Object>> window(Flux<Object> flux) {
            if (every <= 0) {
                return flux.window(span);
            }
            return flux
                .window(span, every);
        }

        public Flux<Flux<Object>> windowTime(Flux<Object> flux) {
            if (getEvery() <= 0) {
                return flux.window(Duration.ofSeconds(getSpan()));
            }
            return flux.window(Duration.ofSeconds(getSpan()), Duration.ofSeconds(getEvery()));
        }
    }


}
