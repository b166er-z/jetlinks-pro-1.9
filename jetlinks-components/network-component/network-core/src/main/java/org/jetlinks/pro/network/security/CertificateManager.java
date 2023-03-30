package org.jetlinks.pro.network.security;

import reactor.core.publisher.Mono;

/**
 * CA证书管理器，用于统一管理证书
 *
 * @author zhouhao
 * @since 1.0
 */
public interface CertificateManager {

    /**
     * 根据ID获取证书信息,如果证书不存在则返回{@link Mono#empty()}
     *
     * @param id ID
     * @return 证书信息
     */
    Mono<Certificate> getCertificate(String id);

}
