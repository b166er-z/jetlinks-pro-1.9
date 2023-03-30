package org.jetlinks.pro.rule.engine.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rule.engine")
public class RuleEngineProperties {

    private String clusterName = "jetlinks";

    private String serverId = "default";

    private String serverName = "default";


}
