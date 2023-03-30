package org.jetlinks.pro.protocol.local;

import com.sun.nio.file.SensitivityWatchEventModifier;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.jetlinks.core.spi.ProtocolSupportProvider;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.supports.protocol.management.jar.ProtocolClassLoader;
import org.springframework.util.ClassUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class LocalFileProtocolSupport implements ProtocolSupport {

    private volatile ProtocolSupport loaded;

    private final Disposable.Composite disposable = Disposables.composite();


    @SneakyThrows
    protected void closeClassLoader(ProtocolClassLoader loader) {
        if (null != loader) {
            loader.close();
        }
    }

    @SneakyThrows
    public void init(File file, ServiceContext context, String providerName) {
        String path = file.isDirectory() ? file.getAbsolutePath() : file.getParentFile().getAbsolutePath();

        WatchService watchService = FileSystems.getDefault().newWatchService();

        Consumer<Path> doWatch = watch -> {
            try {
                WatchKey key = watch.register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE,
                                                  StandardWatchEventKinds.ENTRY_MODIFY,
                                                  StandardWatchEventKinds.ENTRY_DELETE},
                                              SensitivityWatchEventModifier.HIGH);

                disposable.add(key::cancel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Files.walk(Paths.get(path), FileVisitOption.FOLLOW_LINKS)
             .filter(p -> p.toFile().isDirectory())
             .forEach(doWatch);


        AtomicReference<ProtocolClassLoader> ref = new AtomicReference<>();

        disposable.add(new Disposable() {
            @Override
            @SneakyThrows
            public void dispose() {
                watchService.close();
                closeClassLoader(ref.get());
            }
        });

        URL[] urls = new URL[]{file.toURI().toURL()};
        Callable<?> init = () -> {
            log.debug("{}load local protocol :{}", ref.get() == null ? "" : "re", file);
            ProtocolClassLoader loader = new ProtocolClassLoader(
                urls, ClassUtils.getDefaultClassLoader()
            );

            ProtocolSupportProvider supportProvider = (ProtocolSupportProvider) Class
                .forName(providerName, true, loader)
                .newInstance();

            supportProvider.create(context)
                           .subscribe(
                               protocol -> {
                                   log.debug("{}load local protocol :{}", ref.get() == null ? "" : "re", protocol);
                                   closeClassLoader(ref.get());
                                   ref.set(loader);
                                   if (loaded != null) {
                                       loaded.dispose();
                                   }
                                   loaded = protocol;
                               }, error -> {
                                   log.error("init local protocol error", error);
                                   closeClassLoader(loader);
                               });
            return null;
        };
        init.call();

        disposable
            .add(Flux
                     .create(sink -> {
                         while (!isDisposed()) {
                             try {
                                 WatchKey watchKey = watchService.take();
                                 if (watchKey == null) {
                                     continue;
                                 }
                                 watchKey.pollEvents();
                                 sink.next(watchKey);
                                 watchKey.reset();
                             } catch (InterruptedException | ClosedWatchServiceException e) {
                                 break;
                             } catch (Exception e) {
                                 log.error("init local protocol error", e);
                             }
                         }
                     })
                     .elapsed()
                     .window(Duration.ofSeconds(2))
                     .flatMap(window -> window.takeLast(1))
                     .delayElements(Duration.ofSeconds(1))
                     .doOnNext(tp2 -> {
                         try {
                             init.call();
                         } catch (Exception e) {
                             log.error("init local protocol error", e);
                         }
                     })
                     .subscribeOn(Schedulers.elastic())
                     .subscribe()
            );
    }

    @Nonnull
    @Override
    public String getId() {
        return loaded.getId();
    }

    @Override
    public String getName() {
        return loaded.getName();
    }

    @Override
    public String getDescription() {
        return loaded.getDescription();
    }

    @Override
    public Flux<? extends Transport> getSupportedTransport() {
        return loaded.getSupportedTransport();
    }

    @Nonnull
    @Override
    public Mono<? extends DeviceMessageCodec> getMessageCodec(Transport transport) {
        return loaded.getMessageCodec(transport);
    }

    @Nonnull
    @Override
    public DeviceMetadataCodec getMetadataCodec() {
        return loaded.getMetadataCodec();
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request, @Nonnull DeviceOperator deviceOperation) {
        return loaded.authenticate(request, deviceOperation);
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request, @Nonnull DeviceRegistry registry) {
        return loaded.authenticate(request, registry);
    }

    @Override
    public Flux<DeviceMetadataCodec> getMetadataCodecs() {
        return loaded.getMetadataCodecs();
    }

    @Override
    public Mono<ConfigMetadata> getConfigMetadata(Transport transport) {
        return loaded.getConfigMetadata(transport);
    }

    @Override
    public Mono<ConfigMetadata> getInitConfigMetadata() {
        return loaded.getInitConfigMetadata();
    }

    @Override
    public Mono<DeviceMessageSenderInterceptor> getSenderInterceptor() {
        return loaded.getSenderInterceptor();
    }

    @Nonnull
    @Override
    public Mono<DeviceStateChecker> getStateChecker() {
        return loaded.getStateChecker();
    }

    @Override
    public Flux<ConfigMetadata> getMetadataExpandsConfig(Transport transport, DeviceMetadataType metadataType, String metadataId, String dataTypeId) {
        return loaded.getMetadataExpandsConfig(transport, metadataType, metadataId, dataTypeId);
    }

    @Override
    public Mono<Void> onProductRegister(DeviceProductOperator operator) {
        return loaded.onProductRegister(operator);
    }

    @Override
    public Mono<Void> onDeviceRegister(DeviceOperator operator) {
        return loaded.onDeviceRegister(operator);
    }

    @Override
    public Mono<Void> onDeviceUnRegister(DeviceOperator operator) {
        return loaded.onDeviceUnRegister(operator);
    }

    @Override
    public Mono<Void> onProductUnRegister(DeviceProductOperator operator) {
        return loaded.onProductUnRegister(operator);
    }

    @Override
    public Mono<DeviceMetadata> getDefaultMetadata(Transport transport) {
        return loaded.getDefaultMetadata(transport);
    }

    @Override
    public Mono<Void> onDeviceMetadataChanged(DeviceOperator operator) {
        return loaded.onDeviceMetadataChanged(operator);
    }

    @Override
    public Mono<Void> onProductMetadataChanged(DeviceProductOperator operator) {
        return loaded.onProductMetadataChanged(operator);
    }

    @Override
    public void dispose() {
        if (loaded != null) {
            loaded.dispose();
        }
        disposable.dispose();
    }

    @Override
    public boolean isDisposed() {
        return disposable.isDisposed();
    }
}
