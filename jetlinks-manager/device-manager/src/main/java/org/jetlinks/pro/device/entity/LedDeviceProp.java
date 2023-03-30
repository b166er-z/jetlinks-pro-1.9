package org.jetlinks.pro.device.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LedDeviceProp {
    private String productId;
    private String deviceId;
    private String deviceName;
    private List<LedProp> props=new ArrayList<>();

    public void addProp(LedProp ledProp){
        props.add(ledProp);
    }
    public List<LedProp> getProps(){
        return props;
    }
}
