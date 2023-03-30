package org.jetlinks.pro.notify.rule;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.notify.DefaultNotifyType;
import org.springframework.util.Assert;

@Getter
@Setter
public class RuleNotifierProperties {

    private DefaultNotifyType notifyType;

    private String notifierId;

    private String templateId;

    public void validate() {
        Assert.notNull(notifyType, "notifyType can not be null");
        Assert.hasText(notifierId, "notifierId can not be empty");
        Assert.hasText(templateId, "templateId can not be empty");

    }
}
