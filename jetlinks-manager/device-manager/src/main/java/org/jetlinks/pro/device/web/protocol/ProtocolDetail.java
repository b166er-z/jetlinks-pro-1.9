package org.jetlinks.pro.device.web.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ProtocolDetail {
    private String id;

    private String name;

    private List<TransportDetail> transports;

    public static Mono<ProtocolDetail> of(ProtocolSupport support) {
        return support
            .getSupportedTransport()
            .flatMap(trans -> TransportDetail.of(support, trans))
            .collectList()
            .map(details -> new ProtocolDetail(support.getId(), support.getName(), details));
    }
}




