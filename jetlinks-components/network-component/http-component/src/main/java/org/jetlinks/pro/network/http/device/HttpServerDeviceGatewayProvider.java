package org.jetlinks.pro.network.http.device;

import com.alibaba.fastjson.JSONArray;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.core.topic.Router;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.http.server.HttpServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * HTTP 服务设备网关提供商
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
public class HttpServerDeviceGatewayProvider implements DeviceGatewayProvider {
    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler clientMessageHandler;

    private final ProtocolSupports protocolSupports;

    public HttpServerDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler clientMessageHandler,
                                           ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.clientMessageHandler = clientMessageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "http-server-gateway";
    }

    @Override
    public String getName() {
        return "HTTP 推送接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<HttpServer>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(server -> {

                Function<String, Mono<ProtocolSupport>> supplier;

                String protocol = properties.getString("protocol").orElse(null);
                List<String> urls = new ArrayList<>();
                //兼容以前的配置方式,如果指定了协议则全部请求都处理
                if (StringUtils.hasText(protocol)) {
                    supplier = (url) -> protocolSupports.getProtocol(protocol);
                    urls.add("/**");
                } else {
                    //根据url路由获取协议包
                    Router<String, ProtocolSupport> router = Router.create();
                    List<RouteConfig> configs;
                    try {
                        configs = properties.get("routes")
                            .<List<Object>>map(List.class::cast)
                            .map(JSONArray::new)
                            .map(array -> array.toJavaList(RouteConfig.class))
                            .orElseThrow(NullPointerException::new);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("参数routes错误", e);
                    }
                    for (RouteConfig config : configs) {
                        String url = config.getUrl();
                        if (StringUtils.isEmpty(url)) {
                            url = "/**";
                        }
                        urls.add(url);
                        router.route(url, ignore -> protocolSupports.getProtocol(config.protocol));
                    }
                    supplier = url -> router.execute(url, url).take(1)
                                            .singleOrEmpty()
                                            .flatMap(Mono::from)
                                            .doOnNext(proto -> log.debug("use protocol [{}] for http request [{}]", proto
                                                .getId(), url))
                                            .switchIfEmpty(Mono.fromRunnable(() -> log.warn("not found any protocol for http request [{}]", url)))
                    ;
                }

                return new HttpServerDeviceGateway(properties.getId(),
                                                   urls.toArray(new String[0]),
                                                   server,
                                                   supplier,
                                                   sessionManager,
                                                   registry,
                                                   clientMessageHandler);
            });
    }

    @Getter
    @Setter
    public static class RouteConfig {
        private String url;

        private String protocol;
    }
}
