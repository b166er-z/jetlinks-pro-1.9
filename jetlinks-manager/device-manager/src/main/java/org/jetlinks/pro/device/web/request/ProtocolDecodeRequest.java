package org.jetlinks.pro.device.web.request;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.pro.device.entity.ProtocolSupportEntity;

@Getter
@Setter
public class ProtocolDecodeRequest {

    ProtocolSupportEntity entity;

    ProtocolDecodePayload request;

}
