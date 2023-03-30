package org.jetlinks.pro.device.message.streaming;

import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import io.netty.util.internal.ObjectPool;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.DeviceDataManager;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public abstract class DevicePropertyScript extends Script {

    private ObjectPool.Handle<DevicePropertyScript> handle;

    static DeviceDataManager RECENT_DATA_MANAGER;

    private final DeviceDataManager recentDataManager = RECENT_DATA_MANAGER;

    public String getDeviceId() {
        return (String) getProperty("deviceId");
    }

    public long getTimestamp() {
        return (Long) getProperty("timestamp");
    }

    /*======== 内置函数 ======== */
    public long $now() {
        return System.currentTimeMillis();
    }

    public String $now(String format) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    //最近属性数据
    public Object $recent(String property) {
        //本次处理包含了此属性值
        Object propertyValue = getProperty(property);
        if (null != propertyValue) {
            return propertyValue;
        }
        return blockGet(() -> "$recent(" + property + ")", this.$recentAsync(property));
    }

    public Object $recent(String deviceId, String property) {
        return blockGet(() -> "$recent(" + deviceId + "," + property + ")", this.$recentAsync(deviceId, property));
    }

    public Mono<Object> $recentAsync(String property) {
        Object propertyValue = getProperty(property);
        if (null != propertyValue) {
            return Mono.just(propertyValue);
        }

        String deviceId = getDeviceId();
        if (deviceId == null) {
            return Mono.empty();
        }
        return $recentAsync(deviceId, property);
    }

    public Mono<Object> $recentAsync(String deviceId, String property) {
        return $lastStateAsync(deviceId, property);
    }

    //最新上报数据时间
    public Long $recentTime() {
        return blockGet(() -> "$recentTime()", $recentTimeAsync());
    }

    public Long $recentTime(String deviceId) {
        return blockGet(() -> "$recentTime(" + deviceId + ")", $recentTimeAsync(deviceId));
    }

    public Long $recentTime(String deviceId, String property) {
        return blockGet(() -> "$recentTime(" + deviceId + "," + property + ")", $recentTimeAsync(deviceId, property));
    }

    public Mono<Long> $recentTimeAsync() {
        return $recentTimeAsync(getDeviceId());
    }

    public Mono<Long> $recentTimeAsync(String deviceId) {
        return getRecentDataManager().getLastPropertyTime(deviceId, getTimestamp());
    }

    public Mono<Long> $recentTimeAsync(String deviceId, String property) {
        return getRecentDataManager()
            .getLastProperty(deviceId, property, getTimestamp())
            .map(DeviceDataManager.PropertyValue::getTimestamp);
    }

    //指定设备的属性的最近数据
    public Object $lastState(String deviceId, String property) {
        return blockGet(() -> "$lastState(" + deviceId + "," + property + ")", this.$lastStateAsync(deviceId, property));
    }

    public Object $lastState(String property) {
        return $lastState(getDeviceId(), property);
    }

    public Mono<Object> $lastStateAsync(String property) {
        return $lastStateAsync(getDeviceId(), property);
    }

    public Mono<Object> $lastStateAsync(String deviceId, String property) {
        return getRecentDataManager()
            .getLastProperty(deviceId, property, getTimestamp())
            .map(DeviceDataManager.PropertyValue::getValue);
    }

    //首次属性数据
    public Object $first(String property) {
        //本次处理包含了此属性值
        Object propertyValue = getProperty(property);
        if (null != propertyValue) {
            return propertyValue;
        }
        return $recent(getDeviceId(), property);
    }

    //指定设备的属性的最近数据
    public Object $first(String deviceId, String property) {
        return blockGet(() -> "$first(" + deviceId + "," + property + ")", this.$firstAsync(deviceId, property));
    }

    public Mono<Object> $firstAsync(String property) {
        return $recentAsync(getDeviceId(), property);
    }

    public Mono<Object> $firstAsync(String deviceId, String property) {
        return $lastStateAsync(deviceId, property);
    }

    //首次上报数据时间
    public Long $firstTime() {
        return blockGet(() -> "$firstTime()", $firstTimeAsync());
    }

    public Long $firstTime(String deviceId) {
        return blockGet(() -> "$firstTime(" + deviceId + ")", $firstTimeAsync(deviceId));
    }

    public Long $firstTime(String deviceId, String property) {
        return blockGet(() -> "$firstTime(" + deviceId + "," + property + ")", $firstTimeAsync(deviceId, property));
    }

    public Mono<Long> $firstTimeAsync() {
        return $firstTimeAsync(getDeviceId());
    }

    public Mono<Long> $firstTimeAsync(String deviceId) {
        return getRecentDataManager().getFirstPropertyTime(deviceId);
    }

    public Mono<Long> $firstTimeAsync(String deviceId, String property) {
        return getRecentDataManager()
            .getFistProperty(deviceId, property)
            .map(DeviceDataManager.PropertyValue::getTimestamp);
    }

    @Override
    public Object getProperty(String property) {
        // TODO: 2021/4/15 获取常量池

        return super.getProperty(property);
    }

    private DeviceDataManager getRecentDataManager() {
        try {
            Object manager = getBinding().getVariable(DeviceDataManager.class.getName());
            if (manager instanceof DeviceDataManager) {
                return ((DeviceDataManager) manager);
            }
        } catch (MissingPropertyException ignore) {

        }
        return recentDataManager;
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        return super.invokeMethod(name, args);
    }


    public <T> T blockGet(Supplier<String> operation, Mono<T> async) {
        try {
            //fast get
            if (async instanceof Callable) {
                return (T) ((Callable<?>) async).call();
            }
            return async
                .toFuture()
                .get(10, TimeUnit.SECONDS);
        } catch (Throwable e) {
            log.warn("{} error", operation.get(), e);
        }
        return null;
    }

    @Override
    public void print(Object value) {
        log.debug(String.valueOf(value));
    }

    @Override
    public void printf(String format, Object value) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(format, value));
        }
    }

    @Override
    public void printf(String format, Object[] values) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(format, values));
        }
    }

    @Override
    public void println() {

    }

    @Override
    public void println(Object value) {
        log.debug(String.valueOf(value));
    }

    public Logger logger() {
        return log;
    }

    /*========对象池相关======== */

    //释放当前对象
    void release() {
        if (handle != null) {
            handle.recycle(this);
        }
    }

    void setHandle(ObjectPool.Handle<DevicePropertyScript> handle) {
        if (this.handle != null) {
            throw new IllegalStateException();
        }
        this.handle = handle;
    }
}
