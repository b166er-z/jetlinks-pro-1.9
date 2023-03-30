package org.jetlinks.pro.network.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.network.manager.entity.CertificateEntity;
import org.jetlinks.pro.network.security.Certificate;
import org.jetlinks.pro.network.security.CertificateManager;
import org.jetlinks.pro.network.security.DefaultCertificate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Service
public class CertificateService
        extends GenericReactiveCrudService<CertificateEntity, String>
        implements CertificateManager {
    @Override
    public Mono<Certificate> getCertificate(String id) {
        return createQuery()
                .where(CertificateEntity::getId, id)
                .fetchOne()
                .map(entity -> {
                    DefaultCertificate defaultCertificate = new DefaultCertificate(entity.getId(), entity.getName());
                    return entity.getInstance().init(defaultCertificate, entity.getConfigs());
                });
    }
}
