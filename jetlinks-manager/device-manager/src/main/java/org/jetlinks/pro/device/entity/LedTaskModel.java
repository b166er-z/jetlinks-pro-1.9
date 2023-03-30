package org.jetlinks.pro.device.entity;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.device.enums.DeviceState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
public class LedTaskModel {

    private List<LedPushData> task1=new ArrayList<>();
    private List<LedPushData> task3=new ArrayList<>();
    private List<LedPushData> task5=new ArrayList<>();
    private List<LedPushData> task10=new ArrayList<>();

    //不同推送频率的推送任务加入不同的序列中等待
    public Mono<Void> addDataToList(LedEntity entity){
        LedPushData ledData = LedPushData.of(entity.getMetadata());
        Mono<Void> res=removeDataFromList(ledData.getLedId());
        if(entity.getState() == DeviceState.online && entity.getPush()!=0){
            switch(ledData.getFreq()){
                case 1:task1.add(ledData);break;
                case 3:task3.add(ledData);break;
                case 5:task5.add(ledData);break;
                case 10:task10.add(ledData);break;
            }
        }
        return res;
    }

    //从任务模型中移除推送任务
    public Mono<Void> removeDataFromList(String id){
        if(task1.removeIf(data -> data.getLedId().equals(id)) ||
            task3.removeIf(data -> data.getLedId().equals(id)) ||
            task5.removeIf(data -> data.getLedId().equals(id)) ||
            task10.removeIf(data -> data.getLedId().equals(id)));
        return Mono.empty();
    }

}
