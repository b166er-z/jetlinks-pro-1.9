package org.jetlinks.pro.device.entity;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.firmware.UpgradeFirmwareMessageReply;

@Getter
@Setter
public class PushFirmwareResponse {

    private String deviceId;

    private String historyId;

    private String taskId;

    private boolean success;

    private String message;

    public PushFirmwareResponse ok() {

        this.success = true;
        return this;
    }

    public PushFirmwareResponse error(String message) {
        this.success = false;
        this.message = message;
        return this;
    }

    public PushFirmwareResponse error(Throwable error) {
        this.success = false;
        this.message = error.getMessage() == null ? "系统错误" : error.getMessage();

        return this;
    }

    public PushFirmwareResponse with(UpgradeFirmwareMessageReply reply) {
        if(reply.isSuccess()){
            return ok();
        }
        return error(reply.getMessage());
    }

    public static PushFirmwareResponse of(String deviceId,String historyId, String taskId) {
        PushFirmwareResponse response = new PushFirmwareResponse();
        response.setTaskId(taskId);
        response.setDeviceId(deviceId);
        response.setHistoryId(historyId);
        return response;
    }

}
