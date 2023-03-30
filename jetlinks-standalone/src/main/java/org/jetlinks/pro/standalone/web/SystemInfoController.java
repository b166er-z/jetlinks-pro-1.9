package org.jetlinks.pro.standalone.web;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.pro.Version;
import org.jetlinks.pro.standalone.configuration.api.ApiInfoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RequestMapping("/system")
@RestController
@AllArgsConstructor
public class SystemInfoController {

    private final ApiInfoProperties addressProperties;

    @GetMapping("/version")
    @Authorize(ignore = true)
    public Mono<Version> getVersion() {
        return Mono.just(Version.current);
    }

    @GetMapping("/apis")
    @Authorize(ignore = true)
    public Mono<ApiInfoProperties> getApis() {
        return Mono.just(addressProperties);
    }

}
