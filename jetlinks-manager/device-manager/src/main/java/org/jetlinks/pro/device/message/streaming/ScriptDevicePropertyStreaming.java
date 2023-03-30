package org.jetlinks.pro.device.message.streaming;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MissingPropertyException;
import io.netty.util.Recycler;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.pro.gateway.DeviceMessageUtils;
import org.jetlinks.pro.streaming.Streaming;
import org.reactivestreams.Publisher;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(of = "scriptDigest")
public class ScriptDevicePropertyStreaming implements Streaming<DeviceMessage, Object, DeviceMessage> {

    public final static CompilerConfiguration configuration = new CompilerConfiguration(CompilerConfiguration.DEFAULT);

    private static final Set<String> denyClass = new HashSet<>(
        Arrays.asList(System.class.getName(), Runtime.class.getName())
    );

    private static final GroovyClassLoader loader;

    static {
        configuration.setScriptBaseClass("org.jetlinks.pro.device.message.streaming.DevicePropertyScript");
        loader = new GroovyClassLoader(new AccessControlClassLoader(ClassUtils.getDefaultClassLoader()), configuration);
    }

    @Setter
    private Class<DevicePropertyScript> scriptType;

    private String scriptDigest;

    private final Recycler<DevicePropertyScript> pool = new Recycler<DevicePropertyScript>() {

        @Override
        protected DevicePropertyScript newObject(Handle<DevicePropertyScript> handle) {
            try {
                DevicePropertyScript script = scriptType.newInstance();
                script.setHandle(handle);
                return script;
            } catch (Exception e) {
                return (DevicePropertyScript) InvokerHelper.createScript(scriptType, new Binding());
            }
        }
    };


    @SuppressWarnings("all")
    public static ScriptDevicePropertyStreaming create(String script) {
        ScriptDevicePropertyStreaming streaming = new ScriptDevicePropertyStreaming();

        String scriptDigest = DigestUtils.md5Hex(script);
        streaming.scriptDigest = scriptDigest;
        streaming.scriptType = loader.parseClass(script, "DevicePropertyScript_" + DigestUtils.md5Hex(scriptDigest));

        return streaming;
    }

    private static class AccessControlClassLoader extends ClassLoader {
        public AccessControlClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            handleAccess(name);
            return super.loadClass(name, resolve);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            handleAccess(name);
            return super.loadClass(name);
        }

        @SneakyThrows
        private void handleAccess(String name) {
            if (denyClass.contains(name)) {
                for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                    if (stackTraceElement.getClassName().startsWith("DevicePropertyScript")) {
                        throw new IllegalAccessException(name);
                    }
                }
            }
        }
    }

    @Override
    @SuppressWarnings("all")
    public Mono<Object> compute(DeviceMessage data) {
        return Mono
            .defer(() -> {
                DevicePropertyScript instance = getScript();
                boolean needRelease = true;
                try {
                    Binding binding = new Binding() {

                        @Override
                        public Object getVariable(String name) {
                            try {
                                return super.getVariable(name);
                            } catch (MissingPropertyException e) {
                                return null;
                            }
                        }

                        @Override
                        public Object getProperty(String property) {
                            try {
                                return super.getProperty(property);
                            } catch (MissingPropertyException e) {
                                return null;
                            }
                        }
                    };
                    DeviceMessageUtils
                        .tryGetProperties(data)
                        .ifPresent(props -> props.forEach(binding::setVariable));
                    binding.setVariable("message", data);
                    binding.setVariable("deviceId", data.getDeviceId());
                    binding.setVariable("timestamp", data.getTimestamp());
                    binding.setVariable("__timestamp__", data.getTimestamp());
                    instance.setBinding(binding);
                    Object value = instance.run();
                    if (value instanceof Publisher) {
                        needRelease = false;
                        return Mono
                            .from((Publisher) value)
                            .doFinally(s -> instance.release());
                    }
                    if (value == null) {
                        return Mono.empty();
                    }
                    return Mono.just(value);
                } finally {
                    if (needRelease) {
                        instance.release();
                    }
                }
            })
            .subscribeOn(Schedulers.elastic());
    }

    private DevicePropertyScript getScript() {
        try {
            return pool.get();
        } catch (Throwable e) {
            return (DevicePropertyScript) InvokerHelper.createScript(scriptType, new Binding());
        }
    }

    @Override
    public Flux<DeviceMessage> output() {
        return Flux.empty();
    }

    @Override
    public void dispose() {

    }

}
