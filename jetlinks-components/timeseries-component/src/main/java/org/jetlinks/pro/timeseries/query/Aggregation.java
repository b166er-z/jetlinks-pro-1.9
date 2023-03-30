package org.jetlinks.pro.timeseries.query;

public enum Aggregation {

    MIN,
    MAX,
    AVG,
    SUM,
    COUNT,
    FIRST,
    TOP,

    MEDIAN,//中位数
    SPREAD,//差值
    STDDEV,//标准差

    NONE;

}