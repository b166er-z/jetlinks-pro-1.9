package org.jetlinks.pro.network.udp.gateway;

import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.pro.gateway.DeviceGateway;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.pro.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.pro.network.DefaultNetworkType;
import org.jetlinks.pro.network.NetworkManager;
import org.jetlinks.pro.network.NetworkType;
import org.jetlinks.pro.network.udp.UdpSupport;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * UDP设备网关提供商，提供对UDP设备网关对支持
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
public class UdpDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final ProtocolSupports protocolSupports;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final DeviceRegistry registry;

    public UdpDeviceGatewayProvider(NetworkManager networkManager, ProtocolSupports protocolSupports, DeviceSessionManager sessionManager, DecodedClientMessageHandler messageHandler, DeviceRegistry registry) {
        this.networkManager = networkManager;
        this.protocolSupports = protocolSupports;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.registry = registry;
    }

    @Override
    public String getId() {
        return "udp-device-gateway";
    }

    @Override
    public String getName() {
        return "UDP 接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public Mono<UdpDeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<UdpSupport>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(udp -> {
                String protocol = properties.getString("protocol")
                                            .orElseThrow(() -> new IllegalArgumentException("protocol can not be null"));
                return new UdpDeviceGateway(properties.getId(), () -> protocolSupports.getProtocol(protocol),
                                            sessionManager, udp, messageHandler, registry
                );
            });
    }
}
