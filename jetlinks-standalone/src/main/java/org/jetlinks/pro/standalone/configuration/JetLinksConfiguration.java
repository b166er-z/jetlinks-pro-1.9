package org.jetlinks.pro.standalone.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.guava.CaffeinatedGuava;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.authorization.token.redis.RedisUserTokenManager;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.device.DeviceOperationBroker;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceStateChecker;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.ipc.IpcService;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.rpc.RpcServiceFactory;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.monitor.GatewayServerMetrics;
import org.jetlinks.core.server.monitor.GatewayServerMonitor;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.entity.DeviceProductEntity;
import org.jetlinks.pro.device.service.AutoDiscoverDeviceRegistry;
import org.jetlinks.pro.gateway.session.DefaultDeviceSessionManager;
import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
import org.jetlinks.supports.cluster.EventBusDeviceOperationBroker;
import org.jetlinks.supports.cluster.event.RedisClusterEventBroker;
import org.jetlinks.supports.cluster.event.RedisRSocketEventBroker;
import org.jetlinks.supports.cluster.redis.RedisClusterManager;
import org.jetlinks.supports.config.EventBusStorageManager;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.event.EventBroker;
import org.jetlinks.supports.ipc.EventBusIpcService;
import org.jetlinks.supports.protocol.ServiceLoaderProtocolSupports;
import org.jetlinks.supports.protocol.management.ClusterProtocolSupportManager;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.jetlinks.supports.rpc.DefaultRpcServiceFactory;
import org.jetlinks.supports.rpc.EventBusRpcService;
import org.jetlinks.supports.rpc.IpcRpcServiceFactory;
import org.jetlinks.supports.rpc.IpcRpcServiceFactory;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.jetlinks.supports.server.DefaultSendToDeviceMessageHandler;
import org.jetlinks.supports.server.monitor.MicrometerGatewayServerMetrics;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableConfigurationProperties(JetLinksProperties.class)
@Slf4j
public class JetLinksConfiguration {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> webServerFactoryWebServerFactoryCustomizer() {
        //解决请求参数最大长度问题
        return factory -> factory
            .addServerCustomizers(httpServer -> httpServer
                .httpRequestDecoder(spec -> {
                    spec.maxInitialLineLength(10240);
                    spec.maxHeaderSize(10240);
                    return spec;
                }));
    }

    @Bean(initMethod = "start", destroyMethod = "dispose")
    public EventBusDeviceOperationBroker clusterDeviceOperationBroker(ClusterManager clusterManager, EventBus eventBus) {
        return new EventBusDeviceOperationBroker(clusterManager.getCurrentServerId(), eventBus);
    }

    @Bean
    public EventBusStorageManager eventBusStorageManager(ClusterManager clusterManager, EventBus eventBus) {
        return new EventBusStorageManager(clusterManager,
                                          eventBus,
                                          () -> CaffeinatedGuava
                                              .build(
                                                  Caffeine.newBuilder()
                                                          .expireAfterAccess(Duration.ofHours(2))
                                              ));
    }


    @Bean(initMethod = "startup")
    public RedisClusterManager clusterManager(JetLinksProperties properties, ReactiveRedisTemplate<Object, Object> template) {
        return new RedisClusterManager(properties.getClusterName(), properties.getServerId(), template);
    }

    @Bean
    public ClusterDeviceRegistry clusterDeviceRegistry(ProtocolSupports supports,
                                                       ClusterManager manager,
                                                       ConfigStorageManager storageManager,
                                                       DeviceOperationBroker handler) {

        return new ClusterDeviceRegistry(supports,
                                         storageManager,
                                         manager,
                                         handler,
                                         CaffeinatedGuava.build(Caffeine.newBuilder()));
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "jetlinks.device.registry", name = "auto-discover", havingValue = "enabled")
    public AutoDiscoverDeviceRegistry deviceRegistry(ClusterDeviceRegistry registry,
                                                     ReactiveRepository<DeviceInstanceEntity, String> instanceRepository,
                                                     ReactiveRepository<DeviceProductEntity, String> productRepository) {
        return new AutoDiscoverDeviceRegistry(registry, instanceRepository, productRepository);
    }

    @Bean
    public BeanPostProcessor interceptorRegister(ClusterDeviceRegistry registry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DeviceMessageSenderInterceptor) {
                    registry.addInterceptor(((DeviceMessageSenderInterceptor) bean));
                }
                if(bean instanceof DeviceStateChecker){
                    registry.addStateChecker(((DeviceStateChecker) bean));
                }
                return bean;
            }
        };
    }

    @Bean(initMethod = "startup")
    public DefaultSendToDeviceMessageHandler defaultSendToDeviceMessageHandler(JetLinksProperties properties,
                                                                               DeviceSessionManager sessionManager,
                                                                               DeviceRegistry registry,
                                                                               MessageHandler messageHandler,
                                                                               DecodedClientMessageHandler clientMessageHandler) {
        return new DefaultSendToDeviceMessageHandler(properties.getServerId(), sessionManager, messageHandler, registry, clientMessageHandler);
    }


    @Bean
    public GatewayServerMonitor gatewayServerMonitor(JetLinksProperties properties, MeterRegistry registry) {
        GatewayServerMetrics metrics = new MicrometerGatewayServerMetrics(properties.getServerId(), registry);

        return new GatewayServerMonitor() {
            @Override
            public String getCurrentServerId() {
                return properties.getServerId();
            }

            @Override
            public GatewayServerMetrics metrics() {
                return metrics;
            }
        };
    }


    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public DefaultDeviceSessionManager deviceSessionManager(JetLinksProperties properties,
                                                            GatewayServerMonitor monitor,
                                                            DeviceRegistry registry) {
        DefaultDeviceSessionManager sessionManager = new DefaultDeviceSessionManager();
        sessionManager.setGatewayServerMonitor(monitor);
        sessionManager.setRegistry(registry);
        Optional.ofNullable(properties.getTransportLimit()).ifPresent(sessionManager::setTransportLimits);


        return sessionManager;
    }

    @Bean(initMethod = "init")
    @ConditionalOnProperty(prefix = "jetlinks.protocol.spi", name = "enabled", havingValue = "true")
    public ServiceLoaderProtocolSupports serviceLoaderProtocolSupports(ServiceContext serviceContext) {
        ServiceLoaderProtocolSupports supports = new ServiceLoaderProtocolSupports();
        supports.setServiceContext(serviceContext);
        return supports;
    }

    @Bean
    @ConfigurationProperties(prefix = "hsweb.user-token")
    public UserTokenManager userTokenManager(ReactiveRedisOperations<Object, Object> template) {
        return new RedisUserTokenManager(template);
    }

    @Bean
    public ProtocolSupportManager protocolSupportManager(ClusterManager clusterManager) {
        return new ClusterProtocolSupportManager(clusterManager);
    }


    @Bean
    public LazyInitManagementProtocolSupports managementProtocolSupports(ProtocolSupportManager supportManager,
                                                                         ProtocolSupportLoader loader,
                                                                         ClusterManager clusterManager) {
        LazyInitManagementProtocolSupports supports = new LazyInitManagementProtocolSupports();
        supports.setClusterManager(clusterManager);
        supports.setManager(supportManager);
        supports.setLoader(loader);
        return supports;
    }

    @Bean
    public BrokerEventBus eventBus(ObjectProvider<EventBroker> provider,
                                   Scheduler scheduler) {

        BrokerEventBus eventBus = new BrokerEventBus();
        eventBus.setPublishScheduler(scheduler);
        for (EventBroker eventBroker : provider) {
            eventBus.addBroker(eventBroker);
        }

        return eventBus;
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClusterEventBroker redisClusterEventBroker(ClusterManager clusterManager,
                                                           JetLinksProperties jetLinksProperties,
                                                           ReactiveRedisConnectionFactory connectionFactory) {
        if (jetLinksProperties.getEventBus().getRsocket().isEnabled()) {
            return new RedisRSocketEventBroker(clusterManager,
                                               connectionFactory,
                                               jetLinksProperties.getEventBus().getRsocket().getAddress());
        }
        return new RedisClusterEventBroker(clusterManager, connectionFactory);
    }

    @Bean
    public IpcService rpcService(EventBus eventBus) {
        return new EventBusIpcService(ThreadLocalRandom.current().nextInt(100000, 1000000), eventBus);
    }

    @Bean
    public RpcServiceFactory rpcServiceFactory(IpcService ipcService) {
        return new IpcRpcServiceFactory(ipcService);
    }


}
