package org.jetlinks.pro.topic;

import java.util.List;
import java.util.stream.Collectors;

public interface Topics {

    static String org(String orgId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/org/", orgId, topic);
    }

    static String tenant(String tenantId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/tenant/", tenantId, topic);
    }

    static List<String> tenants(List<String> tenants, String topic) {
        return tenants
            .stream()
            .map(id -> tenant(id, topic))
            .collect(Collectors.toList());
    }

    static String deviceGroup(String groupId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/device-group/", groupId, topic);

    }

    static List<String> deviceGroups(List<String> groupIds, String topic) {
        return groupIds
            .stream()
            .map(id -> deviceGroup(id, topic))
            .collect(Collectors.toList());
    }

    static String tenantMember(String memberId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/member/", memberId, topic);
    }

    static List<String> tenantMembers(List<String> members, String topic) {
        return members
            .stream()
            .map(id -> tenantMember(id, topic))
            .collect(Collectors.toList());
    }

    String allDeviceRegisterEvent = "/_sys/registry-device/*/register";
    String allDeviceUnRegisterEvent = "/_sys/registry-device/*/unregister";
    String allDeviceMetadataChangedEvent = "/_sys/registry-device/*/metadata";

    static String deviceRegisterEvent(String deviceId) {
        return registryDeviceEvent(deviceId, "register");
    }

    static String deviceUnRegisterEvent(String deviceId) {
        return registryDeviceEvent(deviceId, "unregister");
    }

    static String deviceMetadataChangedEvent(String deviceId) {
        return registryDeviceEvent(deviceId, "metadata");
    }

    String allProductRegisterEvent = "/_sys/registry-product/*/register";
    String allProductUnRegisterEvent = "/_sys/registry-product/*/unregister";
    String allProductMetadataChangedEvent = "/_sys/registry-product/*/metadata";

    static String productRegisterEvent(String deviceId) {
        return registryProductEvent(deviceId, "register");
    }

    static String productUnRegisterEvent(String deviceId) {
        return registryProductEvent(deviceId, "unregister");
    }

    static String productMetadataChangedEvent(String deviceId) {
        return registryProductEvent(deviceId, "metadata");
    }


    static String registryDeviceEvent(String deviceId, String event) {
        return "/_sys/registry-device/" + deviceId + "/" + event;
    }

    static String registryProductEvent(String deviceId, String event) {
        return "/_sys/registry-product/" + deviceId + "/" + event;
    }

}
