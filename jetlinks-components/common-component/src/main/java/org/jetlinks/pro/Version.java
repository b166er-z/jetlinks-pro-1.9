package org.jetlinks.pro;

import lombok.Getter;
import org.jetlinks.pro.configuration.RunMode;

@Getter
public class Version {

    public static Version current = new Version();

    private final String edition = "pro";

    private final String version = "1.9.0-SNAPSHOT";

    public String getMode() {
        return RunMode.get();
    }

}
