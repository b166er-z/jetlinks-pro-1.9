package org.jetlinks.pro.configuration;

public class RunMode {

    static String mode = System.getProperty("jetlinks.mode", "cluster");

    public static String get() {
        return mode;
    }

    public static boolean isCluster() {
        return "cluster".equals(mode);
    }

    public static boolean isStandalone() {
        return mode.equals("standalone");
    }

    public static boolean isCloud() {
        return "cloud".equals(mode);
    }

}
