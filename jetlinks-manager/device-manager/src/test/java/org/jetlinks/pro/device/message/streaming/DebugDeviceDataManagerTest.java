package org.jetlinks.pro.device.message.streaming;

import org.jetlinks.core.message.DeviceDataManager;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;


class DebugDeviceDataManagerTest {


    @Test
    void test() {
        DebugDeviceDataManager debugDeviceDataManager = new DebugDeviceDataManager();

        debugDeviceDataManager.addProperty("test", "test", 3, 1);
        debugDeviceDataManager.addProperty("test", "test", 2, 2);
        debugDeviceDataManager.addProperty("test", "test", 1, 3);


        debugDeviceDataManager
            .getLastProperty("test", "test")
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        debugDeviceDataManager
            .getLastProperty("test", "test",3)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext(2)
            .verifyComplete();


        debugDeviceDataManager
            .getFistProperty("test", "test")
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext(3)
            .verifyComplete();

        debugDeviceDataManager
            .getFirstPropertyTime("test")
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        debugDeviceDataManager
            .getLastPropertyTime("test",3)
            .as(StepVerifier::create)
            .expectNext(2L)
            .verifyComplete();
    }
}