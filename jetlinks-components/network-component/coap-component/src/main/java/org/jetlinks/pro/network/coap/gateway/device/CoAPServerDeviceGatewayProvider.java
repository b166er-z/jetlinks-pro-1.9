package org.jetlinks.pro.network.coap.gateway.device;

import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.coap.server.CoapServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * CoAP 服务网关提供商,提供创建CoAP服务设备接入的能力
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class CoAPServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DecodedClientMessageHandler messageHandler;

    private final DeviceSessionManager sessionManager;

    private final ProtocolSupports protocolSupports;

    public CoAPServerDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DecodedClientMessageHandler messageHandler,
                                           DeviceSessionManager sessionManager,
                                           ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.messageHandler = messageHandler;
        this.sessionManager = sessionManager;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "coap-server-gateway";
    }

    @Override
    public String getName() {
        return "CoAP 接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<CoapServer>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(server -> {

                String protocol = properties
                    .getString("protocol")
                    .orElseThrow(() -> new IllegalArgumentException("protocol不能为空"));

                return new CoAPServerDeviceGateway(properties.getId(),
                                                   registry,
                                                   server,
                                                   () -> protocolSupports.getProtocol(protocol),
                                                   sessionManager,
                                                   messageHandler);

            });
    }
}
