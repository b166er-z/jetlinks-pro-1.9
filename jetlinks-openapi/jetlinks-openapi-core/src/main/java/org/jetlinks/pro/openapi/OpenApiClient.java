package org.jetlinks.pro.openapi;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class OpenApiClient implements Serializable {

    private String clientId;

    private String clientName;

    private String secureKey;

    private List<String> ipWhiteList;

    private Signature signature;

    private Authentication authentication;

    public boolean verifyIpAddress(String ipAddress) {
        if (CollectionUtils.isEmpty(ipWhiteList) || StringUtils.isEmpty(ipAddress)) {
            return true;
        }
        if (ipAddress.contains(" ")) {
            ipAddress = ipAddress.split("[ ]")[0].trim();
        }
        return ipWhiteList.contains(ipAddress);
    }
}
