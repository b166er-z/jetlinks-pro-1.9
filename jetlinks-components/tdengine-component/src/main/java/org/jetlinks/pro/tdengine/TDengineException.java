package org.jetlinks.pro.tdengine;

public class TDengineException extends RuntimeException {

    private final String sql;

    public TDengineException(String sql, String message) {
        super(message);
        this.sql = sql;
    }

    public TDengineException(String sql, String message, Throwable cause) {
        super(message, cause);
        this.sql = sql;
    }
}
