package org.jetlinks.pro.device.web.request;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.device.entity.ProtocolSupportEntity;

@Getter
@Setter
public class ProtocolEncodeRequest {

    ProtocolSupportEntity entity;

    ProtocolEncodePayload request;


}
