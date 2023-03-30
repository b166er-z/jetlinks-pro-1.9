package org.jetlinks.pro.network.http.executor;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class HttpClienTaskConfig {

    private String clientId;

    @NotNull
    private String uri;

    @NotNull
    private HttpMethod httpMethod;

    private String contentType;

    private PayloadType responsePayloadType;

    private PayloadType requestPayloadType;

    public void validate() {
        Assert.hasText(clientId, "clientId can not be empty!");
    }
}
