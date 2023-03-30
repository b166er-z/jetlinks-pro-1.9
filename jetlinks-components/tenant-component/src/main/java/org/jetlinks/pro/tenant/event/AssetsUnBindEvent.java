package org.jetlinks.pro.tenant.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.event.DefaultAsyncEvent;

import java.util.Collection;


@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class AssetsUnBindEvent extends DefaultAsyncEvent {

    private String tenantId;

    private String assetType;

    private String userId;

    private Collection<String> assetId;

}
