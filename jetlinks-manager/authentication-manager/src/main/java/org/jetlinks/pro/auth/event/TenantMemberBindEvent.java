package org.jetlinks.pro.auth.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.event.DefaultAsyncEvent;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class TenantMemberBindEvent extends DefaultAsyncEvent {

    private String tenantId;

    private List<String> userId;

}
