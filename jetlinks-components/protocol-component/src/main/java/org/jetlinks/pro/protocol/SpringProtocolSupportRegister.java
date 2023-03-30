package org.jetlinks.pro.protocol;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.protocol.StaticProtocolSupports;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Spring静态协议支持注册器，将Spring容器中的协议定义注册到协议管理中
 *
 * @author zhouhao
 * @since 1.8
 */
@Component
public class SpringProtocolSupportRegister extends StaticProtocolSupports {


    private SpringProtocolSupportRegister(EventBus eventBus, ObjectProvider<ProtocolSupport> supports) {
        supports.stream()
                .map(protocol -> new RenameProtocolSupport(
                    protocol.getId(),
                    protocol.getName(),
                    protocol.getDescription(),
                    protocol,
                    eventBus
                )).forEach(this::register);
    }


}
