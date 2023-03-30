package org.jetlinks.pro.network.security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.function.BiFunction;

/**
 * CA证书信息
 *
 * @author zhouhao
 * @since 1.0
 */
public interface Certificate {

    /**
     * @return 证书ID
     */
    String getId();

    /**
     * @return 证书名称
     */
    String getName();

    /**
     * @return 密钥管理工厂
     */
    KeyManagerFactory getKeyManagerFactory();

    /**
     * @return 信任库管理工厂
     */
    TrustManagerFactory getTrustManagerFactory();

    /**
     * 获取指定服务名的X509密钥管理器
     *
     * @param serverName 服务名
     * @return X509KeyManager
     */
    X509KeyManager getX509KeyManager(String serverName);

    /**
     * 获取全部X509密钥管理器
     *
     * @return 密钥管理器
     */
    X509KeyManager[] getX509KeyManagers();

    /**
     * 获取证书链
     *
     * @param serverName 服务名
     * @return
     */
    X509Certificate[] getCertificateChain(String serverName);

    /**
     * @return 获取信任证书信息
     */
    X509Certificate[] getTrustCerts();

    /**
     * 根据名称获取信任管理器
     *
     * @param serverName 服务名称
     * @return 信任管理器集合
     */
    TrustManager[] getTrustManager(String serverName);

     <T> T doWithKeyStore(BiFunction<KeyStore, String, T> function);
}
