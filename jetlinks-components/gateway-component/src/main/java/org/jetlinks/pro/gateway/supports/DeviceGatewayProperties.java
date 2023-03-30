package org.jetlinks.pro.gateway.supports;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.ValueObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class DeviceGatewayProperties  implements ValueObject {

    private String id;

    private String provider;

    private String networkId;

    private Map<String,Object> configuration=new HashMap<>();

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
