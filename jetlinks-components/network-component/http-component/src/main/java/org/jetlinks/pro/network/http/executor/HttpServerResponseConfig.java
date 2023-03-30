package org.jetlinks.pro.network.http.executor;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * HTTP响应配置
 *
 * @author zhouhao
 * @since 1.3
 */
@Getter
@Setter
public class HttpServerResponseConfig {

    /**
     * 固定响应头，如果规则节点输入未指定则以此配置作为响应头
     */
    private Map<String, String> headers;

    /**
     * 状态码，如果规则节点输入未指定则使用此值作为响应状态码
     */
    private int status = 200;


    public void validate() {

    }

}
