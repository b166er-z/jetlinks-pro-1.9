package org.jetlinks.pro.notify.event;

import lombok.*;
import org.jetlinks.pro.notify.NotifyType;
import org.jetlinks.pro.notify.Provider;
import org.jetlinks.pro.notify.template.Template;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerializableNotifierEvent {

    private boolean success;

    @Nullable
    private String errorType;

    @Nullable
    private String cause;

    @Nonnull
    private String notifierId;

    @Nonnull
    private String notifyType;

    @Nonnull
    private String provider;

    @Nullable
    private String templateId;

    @Nullable
    private Template template;

    @Nonnull
    private Map<String,Object> context;
}
