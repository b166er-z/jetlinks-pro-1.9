package org.jetlinks.pro.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.jetlinks.pro.device.entity.FirmwareUpgradeTaskEntity;
import org.jetlinks.pro.device.service.FirmwareUpgradeTaskService;
import org.jetlinks.pro.device.tenant.DeviceAssetType;
import org.jetlinks.pro.tenant.AssetType;
import org.jetlinks.pro.tenant.crud.TenantCorrelatesAccessCrudController;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

@RestController
@RequestMapping("/firmware/upgrade/task")
@Resource(id = "firmware-upgrade-task-manager", name = "固件升级任务管理")
@Tag(name = "固件升级任务")
public class FirmwareUpgradeTaskController implements TenantCorrelatesAccessCrudController<FirmwareUpgradeTaskEntity, String> {

    private final FirmwareUpgradeTaskService taskService;

    private final LocalDeviceInstanceService instanceService;

    public FirmwareUpgradeTaskController(FirmwareUpgradeTaskService taskService,
                                         LocalDeviceInstanceService instanceService) {
        this.taskService = taskService;
        this.instanceService = instanceService;
    }

    @Override
    public FirmwareUpgradeTaskService getService() {
        return taskService;
    }

    /**
     * 批量升级固件下全部设备
     * <pre>
     *
     * POST /firmware/upgrade/task/{id}/all/_deploy
     *
     * </pre>
     *
     * @param id ID
     * @return ID
     */
    @PostMapping("/{id}/all/_deploy")
    @ResourceAction(id = "deploy", name = "发布任务")
    @Operation(summary = "发布升级任务到所有设备")
    public Mono<Void> deployAllDevice(@PathVariable String id) {
        return this.assertPermission(taskService.findById(id))
            .flatMap(taskService::deploy);
    }

    /**
     * 批量升级产品下指定的设备
     *
     * <pre>
     *
     * POST /firmware/upgrade/task/{id}/_deploy
     * Content-Type application/json
     *
     * ["deviceId1","deviceId2"]
     *
     * </pre>
     *
     * @param id     ID
     * @param idList 设备ID列表
     * @return Void
     */
    @PostMapping("/{id}/_deploy")
    @ResourceAction(id = "deploy", name = "发布任务")
    @Operation(summary = "发布升级任务到指定设备")
    public Mono<Void> deploy(@PathVariable String id,
                             @RequestBody Mono<List<String>> idList) {
        return this
            .assertPermission(taskService.findById(id))
            .flatMap(task -> taskService.deploy(task, idList.flatMapMany(instanceService::findById)));
    }

    @Nonnull
    @Override
    public AssetType getAssetType() {
        return DeviceAssetType.product;
    }

    @Nonnull
    @Override
    public Function<FirmwareUpgradeTaskEntity, ?> getAssetIdMapper() {
        return FirmwareUpgradeTaskEntity::getProductId;
    }

    @Nonnull
    @Override
    public String getAssetProperty() {
        return "productId";
    }
}
