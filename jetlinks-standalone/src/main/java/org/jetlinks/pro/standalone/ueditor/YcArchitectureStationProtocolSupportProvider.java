package org.jetlinks.pro.standalone.ueditor;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.core.spi.ProtocolSupportProvider;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import reactor.core.publisher.Mono;

public class YcArchitectureStationProtocolSupportProvider implements ProtocolSupportProvider {
    public Mono<? extends ProtocolSupport> create(ServiceContext context) {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.setId("YcTYJzhjjcz-v1");
        support.setName("云窗通用建筑环境监测站v1");
        support.setDescription("云窗通用建筑环境监测站v1");
        support.setMetadataCodec((DeviceMetadataCodec)new JetLinksDeviceMetadataCodec());
        YcArchitectureStationTcpMessageCodec codec = new YcArchitectureStationTcpMessageCodec();
        support.addMessageCodecSupport((Transport)DefaultTransport.TCP, () -> Mono.just(codec));
        return Mono.just(support);
    }
}

