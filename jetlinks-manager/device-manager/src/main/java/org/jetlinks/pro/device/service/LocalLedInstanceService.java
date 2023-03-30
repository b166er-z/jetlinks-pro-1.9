package org.jetlinks.pro.device.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.pro.device.entity.*;
import org.jetlinks.pro.device.events.LedDeployedEvent;
import org.jetlinks.pro.device.service.data.DeviceDataService;
import org.jetlinks.pro.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.pro.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.pro.elastic.search.service.ElasticSearchService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
@Slf4j
@EnableScheduling
@EnableAsync
public class LocalLedInstanceService extends GenericReactiveCrudService<LedEntity,String> {

    private final DeviceRegistry registry;
    private final DeviceDataService dataService;
    private final ElasticSearchIndexManager indexManager;
    private final ElasticSearchService elasticSearchService;
    private final LedTaskModel ledModel;

    public LocalLedInstanceService(DeviceRegistry registry,
                                   DeviceDataService dataService,
                                   LedTaskModel ledModel,
                                   ElasticSearchService elasticSearchService,
                                   ElasticSearchIndexManager indexManager){
        this.registry = registry;
        this.dataService = dataService;
        this.ledModel = ledModel;
        this.elasticSearchService = elasticSearchService;
        this.indexManager = indexManager;
    }

    /**
     * 新建LED大屏设备
     * @param ledEntity LED大屏实体对象
     * @return 返回实际添加的数据条数
     */
    public Mono<Integer> addNewLed(Mono<LedEntity> ledEntity){
        ledEntity.flatMap(entity -> indexManager
            .putIndex(new DefaultElasticSearchIndexMetadata(entity.getId())
                .addProperty("ledId", StringType.GLOBAL)
                .addProperty("deviceId", StringType.GLOBAL)
                .addProperty("name", StringType.GLOBAL)
                .addProperty("value", StringType.GLOBAL)
                .addProperty("writeCmd", StringType.GLOBAL)
                .addProperty("result", StringType.GLOBAL)
                .addProperty("time", DateTimeType.GLOBAL)
            )).subscribe();
        return insert(ledEntity);
    }

    /**
     * 更新大屏在线状态(online/offline)
     * 之后根据更新后的值更新大屏任务模型
     */
    @EventListener
    public void changeState(LedDeployedEvent event){
        event.async(
            sendNotify(event.getDevices())
            .flatMap(this :: findById)
            .filter(Objects :: nonNull)
            .flatMap(ledModel :: addDataToList)
        );
    }
    private Flux<String> sendNotify(Flux<DeviceInstanceEntity> entities){
        return entities.flatMap(entity -> createUpdate()
            .set(LedEntity :: getState,entity.getState().getValue())
            .where(LedEntity :: getId,entity.getId())
            .execute()
            .thenReturn(entity.getId()));
    }

    @EventListener
    public void deleteLed(EntityDeletedEvent<DeviceInstanceEntity> event){
        event.async(
            sendDelNotify(Flux.fromIterable(event.getEntity()))
                .flatMap(ledModel :: removeDataFromList)
        );
    }
    private Flux<String> sendDelNotify(Flux<DeviceInstanceEntity> entities){
        return entities.map(DeviceInstanceEntity :: getId)
                            .flatMap(did -> createDelete()
                                            .where(LedEntity :: getId, did)
                                            .execute()
                                            .thenReturn(did));
    }

    /**
     * 修改大屏基本信息，并根据push的值决定是否加入/移出ledTaskModel模型
     * @param id 大屏实体ID
     * @param ledEntity 大屏实体对象
     * @return <=0表示修改不成功
     */
    public Mono<Integer> updateLed(String id,Mono<LedEntity> ledEntity){
        return updateById(id,ledEntity)
            .filter(cnt -> cnt>0)
            .flatMap(cnt -> ledEntity.map(ledModel :: addDataToList).thenReturn(cnt));
    }

    /**
     * 读取大屏下发日志记录
     * @param param 分页参数
     * @return 日志记录<br>
     * Field("ledId","LED/设备ID")<br>
     *Field("deviceId","下发设备ID")<br>
     *Field("name","推送的属性名称")<br>
     *Field("value","属性值")<br>
     *Field("writeCmd","实际下发的命令")<br>
     *Field("result","下发结果")<br>
     */
    public Mono<PagerResult<LedLog>> readLog(String ledId,QueryParam param){
        return elasticSearchService.queryPager(ledId,param,LedLog.class);
    }

    /**
     * 推送数据到LED大屏，并记录日志
     * @param taskList 任务列表
     */
    public void sendLed(List<LedPushData> taskList){
        taskList.forEach(ledPushData -> ledPushData.getMetadata().forEach(ledDeviceProp ->{
            String ledId = ledPushData.getLedId();//LED设备ID
            String deviceId = ledDeviceProp.getDeviceId();//待获取属性值的设备ID

            //以下查询设备最新属性值
            dataService.queryEachOneProperties(deviceId, QueryParamEntity.of()).flatMap(dp -> {
                ledDeviceProp.getProps().forEach(ledProp -> {
                        String id = ledProp.getId();//属性ID，唯一，如“temp”
                        String name = ledProp.getName();//属性名称，如“温度”
                        String pos = ledProp.getPos();//显示位置，如“F3”
                        if(id.equals(dp.getProperty())){
                            String lateValue=dp.getValue()+"";
                            String cmd=generateCmd(pos,name,lateValue);
                            if(cmd!=null){
                                registry.getDevice(ledId)
                                    .flatMap(operator -> operator
                                        .messageSender()
                                        .writeProperty()
                                        .messageId(ledId)
                                        .async(true)
                                        .timeout(Duration.ofSeconds(10))
                                        .header("writeCmd",cmd)
                                        .header("sendAndForget",false)
                                        .validate()
                                    )
                                    .flatMapMany(WritePropertyMessageSender :: send)
                                    .filter(WritePropertyMessageReply :: isSuccess)
                                    .flatMap(reply -> {
                                        LedLog log = new LedLog(ledId,deviceId,name,lateValue,cmd,"发送成功!",System.currentTimeMillis());
                                        return elasticSearchService.commit(ledId,log);
                                    })
                                    .onErrorResume(err -> {
                                        LedLog log = new LedLog(ledId,deviceId,name,lateValue,cmd,"超时，设备未回复",System.currentTimeMillis());
                                        return elasticSearchService.commit(ledId,log);
                                    }).subscribe();
                            }
                        }
                });
                return Mono.just(deviceId);
            }).subscribe();
        }));
    }


    private String toHexLen(int len){
        String str= Integer.toHexString(len);
        return str.length()<2?"0"+str:str;
    }

    private String getNum(String str,int length){
        StringBuilder hexStr = new StringBuilder();
        char[] cs=str.toCharArray();
        for(char c:cs){
            hexStr.append(Integer.toHexString((byte)c));
        }
        if(hexStr.length() == length*2) return hexStr.toString();
        else if (hexStr.length()< length*2) {
            int cha = length*2 - hexStr.length();
            int cha2 = cha/2;
            StringBuilder s = new StringBuilder();
            for(int i=0;i<cha2;i++)s.append("30");
            return s.append(hexStr).toString();
        } else {
            int cha = hexStr.length()-length*2;
            return hexStr.substring(cha);
        }
    }

    private String lastLED(String hex){
        String yHex = hex.substring(2);
        int count = yHex.length()/2;
        long ll = -1;
        for(int i=0;i<count;i++){
            String ahex = yHex.substring(i*2,(i+1)*2);
            long l = Long.parseLong(ahex,16);
            if(ll==-1)ll = l;
            else ll=ll^l;
        }
        return String.format("%02x", ll);
    }

    public String generateCmd(String pos,String name,String value){
        if(value!=null&&pos!=null){
            int lenth = value.length();
            String lengthHex = toHexLen(lenth);
            //判断是否为数字
            if(value.matches("-?[0-9]+.?[0-9]*")){
                if(value.contains(".")){
                    String hex3 = "FE9800$$9737000000000000000000"+pos+"00FFFF"+lengthHex+"XX2EYY";
                    String awd1 = value.split("[.]")[0];
                    String awd2 = value.split("[.]")[1];
                    String awd11Hex = getNum(awd1,awd1.length());
                    String awd22Hex = getNum(awd2,awd2.length());
                    hex3 = hex3.replace("XX",awd11Hex);
                    hex3 = hex3.replace("YY",awd22Hex);
                    hex3 = hex3.replace("$$",String.format("%02x", ((hex3.length()/2)-1)));
                    //last
                    return hex3+lastLED(hex3);
                } else {
                    String hex3 = "FE9800$$9737000000000000000000"+pos+"00FFFF"+lengthHex+"XX";
                    String awd11Hex = getNum(value,value.length());
                    hex3 = hex3.replace("XX",awd11Hex);
                    hex3 = hex3.replace("$$",String.format("%02x", ((hex3.length()/2)-1)));
                    //last
                    return hex3+lastLED(hex3);
                }
            } else {
                String fxv = "";
                if(value.length()==2||value.length()==4){
                    if("正北".equals(value))fxv = "d5fdb1b1";
                    else if("东北".equals(value))fxv = "b6abb1b1";
                    else if("正东".equals(value))fxv = "d5fdb6ab";
                    else if("东南".equals(value))fxv = "b6abc4cf";
                    else if("正南".equals(value))fxv = "d5fdc4cf";
                    else if("西南".equals(value))fxv = "cef7c4cf";
                    else if("正西".equals(value))fxv = "d5fdcef7";
                    else if("西北".equals(value))fxv = "cef7b1b1";
                    else if("东北偏北".equals(value))fxv = "b6abb1b1c6abb1b1";
                    else if("东北偏东".equals(value))fxv = "b6abb1b1c6abb6ab";
                    else if("东南偏东".equals(value))fxv = "b6abc4cfc6abb6ab";
                    else if("东南偏南".equals(value))fxv = "b6abc4cfc6abc4cf";
                    else if("西南偏南".equals(value))fxv = "cef7c4cfc6abc4cf";
                    else if("西南偏西".equals(value))fxv = "cef7c4cfc6abcef7";
                    else if("西北偏西".equals(value))fxv = "cef7b1b1c6abcef7";
                    else if("西北偏北".equals(value))fxv = "cef7b1b1c6abb1b1";
                    else if("无风".equals(value))fxv = "cedeb7e7";
                    else if("打开".equals(value))fxv = "b4f2bfaa";
                    else if("关闭".equals(value))fxv = "b9d8b1d5";
                    if(!"".equals(fxv)){
                        String hex5 = "FE9800$$9737000000000000000000"+pos+"00FFFF04"+fxv;
                        //length
                        int ln = (hex5.length()/2)-1;
                        String lnn = String.format("%02x",ln);
                        hex5 = hex5.replace("$$",lnn);
                        //last
                        return hex5+lastLED(hex5);
                    }
                }
                if(value.length()==1){
                    if("有".equals(value))fxv = "d3d0";
                    else if("无".equals(value))fxv = "cede";
                    else if("开".equals(value))fxv = "bfaa";
                    else if("关".equals(value))fxv = "b9d8";
                    if(!"".equals(fxv)){
                        String hex5 = "FE9800$$9737000000000000000000"+pos+"00FFFF02"+fxv;
                        //length
                        int ln = (hex5.length()/2)-1;
                        String lnn = String.format("%02x",ln);
                        hex5 = hex5.replace("$$",lnn);
                        //last
                        return hex5+lastLED(hex5);
                    }
                }
            }
        }
        return null;
    }

    @Async("taskExecutor")
    @Scheduled(fixedDelay = 10000)
    @SneakyThrows
    public void sendLed_1(){
        sendLed(ledModel.getTask1());
    }

    @Async("taskExecutor")
    @Scheduled(fixedDelay = 1000 * 60 * 3)
    @SneakyThrows
    public void sendLed_3(){
        sendLed(ledModel.getTask3());
    }

    @Async("taskExecutor")
    @Scheduled(fixedDelay = 1000 * 60 * 5)
    @SneakyThrows
    public void sendLed_5(){
        sendLed(ledModel.getTask5());
    }

    @Async("taskExecutor")
    @Scheduled(fixedDelay = 1000 * 60 * 10)
    @SneakyThrows
    public void sendLed_10(){
        sendLed(ledModel.getTask10());
    }

}
