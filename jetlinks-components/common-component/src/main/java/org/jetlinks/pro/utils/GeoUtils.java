package org.jetlinks.pro.utils;

import org.jetlinks.core.metadata.types.GeoPoint;

public class GeoUtils {
    private static final double EARTH_RADIUS = 6378.137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 通过经纬度获取距离(单位：米)
     *
     * @param from from
     * @param to   to
     * @return 距离
     */
    public static double getDistance(GeoPoint from, GeoPoint to) {
        double radLat1 = rad(from.getLat());
        double radLat2 = rad(to.getLat());
        double a = radLat1 - radLat2;
        double b = rad(from.getLon()) - rad(to.getLon());
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
            + Math.cos(radLat1) * Math.cos(radLat2)
            * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d;
        s = s * 1000;
        return s;
    }

}
