package org.jetlinks.pro.device;

import com.alibaba.fastjson.JSON;
import org.jetlinks.pro.device.entity.LedPushData;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestCases {

    @Test
    void test1(){
        try{
            char[] data=new char[3000];
            BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("d:/led-metadata.json"))));
            int len=br.read(data);
            LedPushData obj=JSON.parseObject(new String(data,0,len),LedPushData.class);

            System.out.println(obj.getLedId());
            obj.getMetadata().forEach(ledDeviceProp ->{
                System.out.println("\t"+ledDeviceProp.getProductId());
                System.out.println("\t" + ledDeviceProp.getDeviceId());
                System.out.println("\t" + ledDeviceProp.getDeviceName());
                ledDeviceProp.getProps().forEach(ledProp ->{
                    System.out.println("\t\t" + ledProp.getId());
                    System.out.println("\t\t" + ledProp.getName());
                    System.out.println("\t\t" + ledProp.getPos());
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toGBK(String str) throws Exception{
        StringBuilder sb = new StringBuilder();
        byte[] bytes = str.getBytes("GBK");
        for(byte b : bytes){
            sb.append("%").append(Integer.toHexString((b & 0xff)));
        }
        return sb.toString().replace("%","");
    }
    private byte[] encodeCmd(String readCmd){
        int len = (readCmd.length() / 2);
        byte[] result = new byte[len];
        char[] chs = readCmd.toCharArray();
        for(int i = 0;i < len;i++) result[i] = (byte)(toByte(chs[i * 2]) << 4 | toByte(chs[i * 2 + 1]));
        return result;
    }
    private byte toByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
    private byte[] toBytes(String str) {
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[str.length() / 2];
        for(int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }
    @Test
    void test2(){
        try{
            System.out.println(toGBK("开"));
            System.out.println(toGBK("关"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
