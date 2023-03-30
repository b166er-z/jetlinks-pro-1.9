package org.jetlinks.pro.device.entity;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LedPushData {
    private String metadataJson;
    private String ledId;
    private int freq;
    private List<LedDeviceProp> metadata = new ArrayList<>();

    public void addDevice(LedDeviceProp ldp){
        metadata.add(ldp);
    }

    public void clearDevice(){
        metadata.clear();
    }

    public static LedPushData of(String json){
        return JSON.parseObject(json,LedPushData.class);
    }
}
