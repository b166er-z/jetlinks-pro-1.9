package org.jetlinks.pro.network.coap.client;

import lombok.SneakyThrows;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.jetlinks.pro.network.security.Certificate;
import org.jetlinks.pro.network.security.DefaultCertificate;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import reactor.test.StepVerifier;

import javax.net.ssl.X509KeyManager;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.Collections;

class DefaultCoapClientTest {

    static {
        NetworkConfig.setStandard(new NetworkConfig());
    }

    public static void main(String[] args) {

        CoapServer server = new CoapServer();


        server.addEndpoint(new CoapEndpoint.Builder()
                .setPort(12345)
                .build());
        server.add(new CoapResource("test") {
            @Override
            public void handlePOST(CoapExchange exchange) {
                System.out.println(exchange.getRequestOptions().toString());
                exchange.respond("ok");
            }

            @Override
            public Resource getChild(String name) {
                return this;
            }
        });

        server.start();

    }

    @Test
    void test() {

        CoapServer server = new CoapServer();


        server.addEndpoint(new CoapEndpoint.Builder()
                .setPort(12345)
                .build());
        server.add(new CoapResource("test") {
            @Override
            public void handlePOST(CoapExchange exchange) {

                exchange.respond("ok");
            }
        });

        CoapClient client = new CoapClient();
        client.setEndpoint(server.getEndpoint(12345));
        CoapClientProperties properties = new CoapClientProperties();

        DefaultCoapClient coapClient = new DefaultCoapClient(client, properties);
        coapClient.publish(Request.newPost().setURI("coaps://localhost:12345/test").setPayload("test"))
                .as(StepVerifier::create)
                .expectNextMatches(resp -> resp.isSuccess() && resp.getResponseText().equals("ok"))
                .verifyComplete();

    }


    @Test
    @SneakyThrows
    void testPem() {
        DefaultCertificate serverCert = new DefaultCertificate("server", "test");
        DefaultCertificate clientCert = new DefaultCertificate("client", "test");

        byte[] serverKs = StreamUtils.copyToByteArray(new ClassPathResource("server.pem").getInputStream());
        byte[] serverCer = StreamUtils.copyToByteArray(new ClassPathResource("server.csr").getInputStream());

        byte[] clientKs = StreamUtils.copyToByteArray(new ClassPathResource("client.pem").getInputStream());
        byte[] clientCer = StreamUtils.copyToByteArray(new ClassPathResource("client.csr").getInputStream());

        byte[] trust = StreamUtils.copyToByteArray(new ClassPathResource("trustStore.pem").getInputStream());

        serverCert.initPemKey(Collections.singletonList(serverKs), Collections.singletonList(serverCer))
                .initPemTrust(Collections.singletonList(trust));
        clientCert.initPemKey(Collections.singletonList(clientKs), Collections.singletonList(clientCer))
                .initPemTrust(Collections.singletonList(trust))
        ;

        testDtls(serverCert, clientCert);
    }

    @Test
    @SneakyThrows
    void testP12() {
        DefaultCertificate serverCert = new DefaultCertificate("server", "test");
        DefaultCertificate clientCert = new DefaultCertificate("client", "test");

        byte[] serverKs = StreamUtils.copyToByteArray(new ClassPathResource("server.p12").getInputStream());

        byte[] clientKs = StreamUtils.copyToByteArray(new ClassPathResource("client.p12").getInputStream());

        byte[] trust = StreamUtils.copyToByteArray(new ClassPathResource("trustStore.p12").getInputStream());

        serverCert.initPfxKey(serverKs, "endPass").initPfxTrust(trust, "rootPass");
        clientCert.initPfxKey(clientKs, "endPass").initPfxTrust(trust, "rootPass");

        testDtls(serverCert, clientCert);
    }

    @Test
    @SneakyThrows
    void testJksPem() {
        DefaultCertificate serverCert = new DefaultCertificate("test", "test");
        byte[] serverKs = StreamUtils.copyToByteArray(new ClassPathResource("keyStore.jks").getInputStream());
        byte[] trust = StreamUtils.copyToByteArray(new ClassPathResource("trustStore.jks").getInputStream());

        serverCert.initJksKey(serverKs, "endPass")
                .initJksTrust(trust, "rootPass");
        DefaultCertificate clientCert = new DefaultCertificate("client", "test");

        byte[] clientKs = StreamUtils.copyToByteArray(new ClassPathResource("client.pem").getInputStream());
        byte[] clientCer = StreamUtils.copyToByteArray(new ClassPathResource("client.csr").getInputStream());

        byte[] clientTrust = StreamUtils.copyToByteArray(new ClassPathResource("trustStore.pem").getInputStream());

        clientCert.initPemKey(Collections.singletonList(clientKs), Collections.singletonList(clientKs))
                .initPemTrust(Collections.singletonList(clientTrust))
        ;
        testDtls(serverCert, clientCert);
    }

    @Test
    @SneakyThrows
    void testJks() {
        DefaultCertificate serverCert = new DefaultCertificate("test", "test");
        byte[] serverKs = StreamUtils.copyToByteArray(new ClassPathResource("keyStore.jks").getInputStream());
        byte[] trust = StreamUtils.copyToByteArray(new ClassPathResource("trustStore.jks").getInputStream());

        serverCert.initJksKey(serverKs, "endPass")
                .initJksTrust(trust, "rootPass");

        testDtls(serverCert, serverCert);
    }

    @SneakyThrows
    void testDtls(Certificate serverCert, Certificate clientCert) {
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );

        X509KeyManager keyManager = serverCert.getX509KeyManager("server");

        PrivateKey privateKey = keyManager.getPrivateKey(null);

        X509KeyManager clientKeyManager = clientCert.getX509KeyManager("client");

        PrivateKey clientPrivateKey = clientKeyManager.getPrivateKey(null);

        CoapServer server = new CoapServer();
        try {
            DTLSConnector connector = new DTLSConnector(new DtlsConnectorConfig.Builder()
                    .setServerOnly(true)
                    .setAddress(new InetSocketAddress(12346))
                    .setPskStore(new InMemoryPskStore())
                    .setIdentity(privateKey,
                            keyManager.getCertificateChain(null), CertificateType.RAW_PUBLIC_KEY, CertificateType.X_509)
                    .setTrustCertificateTypes(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY)
                    .setCertificateVerifier(new StaticCertificateVerifier(serverCert.getTrustCerts()))
                    .setRpkTrustAll()
                    .build());

            server.addEndpoint(new CoapEndpoint.Builder()
                    .setConnector(connector)
                    .build());

            server.add(new CoapResource("test") {
                @Override
                public void handleGET(CoapExchange exchange) {
                    exchange.respond("ok");
                }
            });

            server.start();

            DTLSConnector clientConnector = new DTLSConnector(new DtlsConnectorConfig.Builder()
                    .setClientOnly()
                    .setPskStore(new InMemoryPskStore())
                    .setIdentity(clientPrivateKey,
                            clientKeyManager.getCertificateChain(null), CertificateType.RAW_PUBLIC_KEY, CertificateType.X_509)
                    .setTrustCertificateTypes(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY)
                    .setCertificateVerifier(new StaticCertificateVerifier(clientCert.getTrustCerts()))
                    .setRpkTrustAll()
                    .build());
            CoapClient client = new CoapClient();

            client.setEndpoint(new CoapEndpoint.Builder()
                    .setConnector(clientConnector)
                    .build());

            Assert.assertEquals(client.setURI("coaps://localhost:12346/test").get().getResponseText(), "ok");
        } finally {
            server.stop();
        }

    }
}