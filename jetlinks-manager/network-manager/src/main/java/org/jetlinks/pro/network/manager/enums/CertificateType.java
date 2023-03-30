package org.jetlinks.pro.network.manager.enums;

import lombok.Getter;
import org.jetlinks.pro.network.manager.entity.CertificateEntity;
import org.jetlinks.pro.network.security.DefaultCertificate;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author wangzheng
 * @since 1.0
 */
@Getter
public enum CertificateType {
    PFX {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            if (StringUtils.hasText(config.getKeystoreBase64())) {
                certificate
                    .initPfxKey(Base64.getDecoder().decode(config.getKeystoreBase64()), config.getKeystorePwd());
            }
            return certificate
                .initPfxTrust(Base64
                                  .getDecoder()
                                  .decode(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    JKS {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            if (StringUtils.hasText(config.getKeystoreBase64())) {
                certificate
                    .initJksKey(Base64.getDecoder().decode(config.getKeystoreBase64()), config.getKeystorePwd());
            }
            return certificate
                .initJksTrust(Base64
                                  .getDecoder()
                                  .decode(config.getTrustKeyStoreBase64()), config.getTrustKeyStorePwd());
        }
    },
    PEM {
        @Override
        public DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config) {
            if (StringUtils.hasText(config.getKeystoreBase64())) {
                List<byte[]> keyCert = Collections.singletonList(Base64
                                                                     .getDecoder()
                                                                     .decode(config.getKeystoreBase64()));
                certificate.initPemKey(keyCert, keyCert);
            }

            return certificate
                .initPemTrust(Collections.singletonList(Base64.getDecoder().decode(config.getTrustKeyStoreBase64())));

        }
    };

    public abstract DefaultCertificate init(DefaultCertificate certificate, CertificateEntity.CertificateConfig config);
}
