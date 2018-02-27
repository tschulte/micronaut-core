/*
 * Copyright 2017 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.http.server.netty.ssl;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.particleframework.http.server.HttpServerConfiguration;
import org.particleframework.http.server.ssl.ClientAuthentication;
import org.particleframework.http.server.ssl.SslBuilder;
import org.particleframework.http.server.ssl.SslConfigurationException;

import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;

/**
 * The Netty implementation of {@link SslBuilder} that generates
 * an {@link SslContext} to create a handler with to support SSL.
 *
 * @author James Kleeh
 * @since 1.0
 */
@Singleton
public class NettySslBuilder extends SslBuilder<SslContext> {

    public NettySslBuilder(HttpServerConfiguration.SslConfiguration ssl) {
        super(ssl);
    }

    @Override
    public Optional<SslContext> build() {
        if (!ssl.isEnabled()) {
            return Optional.empty();
        }
        if (ssl.buildSelfSigned()) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                return Optional.of(SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build());
            } catch (CertificateException | SSLException e) {
                throw new SslConfigurationException("Encountered an error while building a self signed certificate", e);
            }
        }
        SslContextBuilder sslBuilder = SslContextBuilder
                .forServer(getKeyManagerFactory())
                .trustManager(getTrustManagerFactory());
        if (ssl.getProtocols().isPresent()) {
            sslBuilder.protocols(ssl.getProtocols().get());
        }
        if (ssl.getCiphers().isPresent()) {
            sslBuilder = sslBuilder.ciphers(Arrays.asList(ssl.getCiphers().get()));
        }
        if (ssl.getClientAuthentication().isPresent()) {
            ClientAuthentication clientAuth = ssl.getClientAuthentication().get();
            if (clientAuth == ClientAuthentication.NEED) {
                sslBuilder = sslBuilder.clientAuth(ClientAuth.REQUIRE);
            } else if (clientAuth == ClientAuthentication.WANT) {
                sslBuilder = sslBuilder.clientAuth(ClientAuth.OPTIONAL);
            }
        }

        try {
            return Optional.of(sslBuilder.build());
        } catch (SSLException ex) {
            throw new SslConfigurationException("An error occurred while setting up SSL", ex);
        }
    }
}
