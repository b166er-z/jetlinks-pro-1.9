package org.jetlinks.pro.utils;

public class SystemUtils {

    private static final float memoryWatermark =  Float.parseFloat(
        System.getProperty("memory.watermark", System.getProperty("memory.waterline", "0.12")));

    //水位线持续
    private static final long memoryWatermarkDuration = TimeUtils
        .parse(System.getProperty("memory.watermark.duration", "5s"))
        .toMillis();

    /**
     * 获取内存剩余比例,值为0-1之间,值越小,剩余可用内存越小
     *
     * @return 内存剩余比例
     */
    public static float getMemoryRemainder() {
        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory();
        long total = rt.totalMemory();
        long max = rt.maxMemory();
        return (max - total + free) / (max + 0.0F);
    }

    private static volatile long outTimes = 0;

    /**
     * 判断当前内存是否已经超过水位线
     *
     * @return 是否已经超过水位线
     */
    public static boolean memoryIsOutOfWatermark() {
        boolean out = getMemoryRemainder() < memoryWatermark;
        if (!out) {
            outTimes = 0;
            return false;
        }
        if (outTimes == 0) {
            outTimes = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - outTimes > memoryWatermarkDuration) {
            //连续超水位线
            return true;
        }
        return false;
    }

}
