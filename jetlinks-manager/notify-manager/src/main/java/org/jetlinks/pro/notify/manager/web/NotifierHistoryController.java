package org.jetlinks.pro.notify.manager.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.pro.notify.manager.entity.NotifyHistoryEntity;
import org.jetlinks.pro.notify.manager.service.NotifyHistoryService;
import org.jetlinks.pro.notify.manager.tenant.NotifyAssetType;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.crud.TenantCorrelatesAccessQueryController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import java.util.function.Function;

@RestController
@RequestMapping("/notify/history")
@Resource(id = "notifier", name = "通知管理")
@Tag(name = "消息通知记录")
public class NotifierHistoryController implements TenantCorrelatesAccessQueryController<NotifyHistoryEntity, String> {

    private final NotifyHistoryService historyService;

    public NotifierHistoryController(NotifyHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public NotifyHistoryService getService() {
        return historyService;
    }

    @Nonnull
    @Override
    public AssetType getAssetType() {
        return NotifyAssetType.notifyConfig;
    }

    @Nonnull
    @Override
    public Function<NotifyHistoryEntity, ?> getAssetIdMapper() {
        return NotifyHistoryEntity::getNotifierId;
    }

    @Nonnull
    @Override
    public String getAssetProperty() {
        return "notifierId";
    }
}
