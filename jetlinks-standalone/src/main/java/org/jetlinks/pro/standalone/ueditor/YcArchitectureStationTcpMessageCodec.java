package org.jetlinks.pro.standalone.ueditor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YcArchitectureStationTcpMessageCodec implements DeviceMessageCodec {
    private static final Logger log = LoggerFactory.getLogger(YcArchitectureStationTcpMessageCodec.class);

    public Transport getSupportTransport() {
        return (Transport)DefaultTransport.TCP;
    }

    private static ConcurrentHashMap<Integer, String> messagesIds = new ConcurrentHashMap<>(16);

    public Publisher<? extends Message> decode(MessageDecodeContext context) {
        return (Publisher<? extends Message>)Flux.defer(() -> {
            FromDeviceMessageContext ctx = (FromDeviceMessageContext)context;
            ByteBuf byteBuf = context.getMessage().getPayload();
            byte[] payload = ByteBufUtil.getBytes(byteBuf);
            System.out.println(Arrays.toString(payload)+"序列化后的数据格式打印");
            String text = ByteBufUtil.hexDump(payload);
            System.out.println(text+"这是复制修改的class文件");

            ReportPropertyMessage message = new ReportPropertyMessage();
            message.setDeviceId(text.substring(0, 12).toUpperCase());
            long time = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
            message.setTimestamp(time);
            String datas = text.substring(12, text.length());
            datas = datas.toUpperCase();
            DeviceSession session = ctx.getSession();
            if (datas.startsWith("FF")) {
                if (session != null && session.getOperator() == null) {
                    DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
                    onlineMessage.setDeviceId(text.substring(0, 12).toUpperCase());
                    onlineMessage.setTimestamp(System.currentTimeMillis());
                    session.setKeepAliveTimeout(Duration.ofSeconds(90L));
                    return (Publisher)Mono.just(onlineMessage);
                }
                return (Publisher)Mono.empty();
            }
            Map<String, Object> properties = new HashMap<>();
            int type = Integer.parseInt(datas.substring(0, 2), 16);
            if (type == 1) {
                double WindSpeed = Integer.parseInt(datas.substring(8, 12), 16);
                properties.put("WindSpeed", Double.valueOf(WindSpeed));
            } else if (type == 2) {
                double WindDirection = Integer.parseInt(datas.substring(8, 12), 16);
                properties.put("WindDirection", Double.valueOf(WindDirection));
            } else if (type == 3) {
                double Humidity = (Integer.parseInt(datas.substring(8, 12), 16) / 10);
                double Temperature = (Integer.parseInt(datas.substring(12, 16), 16) / 10);
                if (Temperature > 3000.0D)
                    Temperature -= 6553.6D;
                double PM25 = Integer.parseInt(datas.substring(24, 28), 16);
                properties.put("Humidity", Double.valueOf(Humidity));
                properties.put("Temperature", Double.valueOf(Temperature));
                properties.put("PM25", Double.valueOf(PM25));
            } else if (type == 4) {
                double PM10 = Integer.parseInt(datas.substring(44, 48), 16);
                float NoiseVolume = (Integer.parseInt(datas.substring(56, 60), 16) / 10);
                properties.put("NoiseVolume", Float.valueOf(NoiseVolume));
                properties.put("PM10", Double.valueOf(PM10));
            } else if (type == 5 && datas.length() >= 52) {
                double RelativeHumidity = Integer.parseInt(datas.substring(8, 12), 16) / 10.0D;
                double Temperature = Integer.parseInt(datas.substring(12, 16), 16) / 10.0D;
                if (Temperature > 3000.0D)
                    Temperature -= 6553.6D;
                double PM25 = Integer.parseInt(datas.substring(24, 28), 16);
                double PM10 = Integer.parseInt(datas.substring(44, 48), 16);
                double HCHO = Integer.parseInt(datas.substring(52, 56), 16) / 100.0D;
                properties.put("Temperature", Double.valueOf(Temperature));
                properties.put("Humidity", Double.valueOf(RelativeHumidity));
                properties.put("PM10", Double.valueOf(PM10));
                properties.put("PM25", Double.valueOf(PM25));
                properties.put("HCHO", Double.valueOf(HCHO));
            } else if (type == 255) {

            }
            message.setProperties(properties);
            if (session != null && session.getOperator() == null) {
                DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
                onlineMessage.setDeviceId(text.substring(0, 12).toUpperCase());
                onlineMessage.setTimestamp(System.currentTimeMillis());
                return (Publisher)Flux.just((Object[])new CommonDeviceMessage[] { (CommonDeviceMessage)message, (CommonDeviceMessage)onlineMessage });
            }
            return (Publisher)Mono.just(message);
        });
    }

    public Publisher<? extends EncodedMessage> encode(MessageEncodeContext context) {
        CommonDeviceMessage message = (CommonDeviceMessage)context.getMessage();
        EncodedMessage encodedMessage = EncodedMessage.simple(Unpooled.wrappedBuffer(message.toString().getBytes()));
        if (message instanceof ReadPropertyMessage) {
            ReadPropertyMessage readPropertyMessage = (ReadPropertyMessage)message;
            byte[] bytes = readPropertyMessage.toString().getBytes();
            encodedMessage = EncodedMessage.simple(Unpooled.wrappedBuffer(bytes));
        } else {
            if (message instanceof FunctionInvokeMessage) {
                List<FunctionParameter> functionParameters;
                Iterator<FunctionParameter> iterator;
                StringBuilder cmd = new StringBuilder();
                FunctionInvokeMessage functionInvokeMessage = (FunctionInvokeMessage)message;
                switch (functionInvokeMessage.getFunctionId()) {
                    case "downDemo":
                        cmd.append("fe");
                        functionParameters = functionInvokeMessage.getInputs();
                        for (FunctionParameter param : functionParameters) {
                            if (param.getName().equals("switchPosition")) {
                                cmd.append(String.format("%04x", new Object[] { Integer.valueOf(((Integer)param.getValue()).intValue()) }));
                                break;
                            }
                        }
                        iterator = functionParameters.iterator();
                        while (true) {
                            if (iterator.hasNext()) {
                                FunctionParameter param = iterator.next();
                                if (param.getName().equals("switchAction")) {
                                    switch (((Integer)param.getValue()).intValue()) {
                                        case 0:
                                            cmd.append(String.format("%04x", new Object[] { Integer.valueOf(0) }));
                                            break;
                                        case 1:
                                            cmd.append(String.format("%04x", new Object[] { Integer.valueOf(1) }));
                                            break;
                                        case 2:
                                            cmd.append(String.format("%04x", new Object[] { Integer.valueOf(2) }));
                                            break;
                                    }
                                } else {
                                    continue;
                                }
                            } else {
                                break;
                            }
                            try {
                                cmd = new StringBuilder();
                                cmd.append("190500005A00F4B2");
                                encodedMessage = EncodedMessage.simple(Unpooled.wrappedBuffer(Hex.decodeHex(cmd.toString())));
                            } catch (DecoderException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
            } else {
                return (Publisher<? extends EncodedMessage>)Mono.just(encodedMessage);
            }
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("190500005A00F4B2");
                encodedMessage = EncodedMessage.simple(Unpooled.wrappedBuffer(Hex.decodeHex(stringBuilder.toString())));
            } catch (DecoderException decoderException) {
                decoderException.printStackTrace();
            }
        }
        return (Publisher<? extends EncodedMessage>)Mono.just(encodedMessage);
    }

    public Mono<? extends MessageCodecDescription> getDescription() {
        return null;
    }
}
