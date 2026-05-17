package com.example.javamate.config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.netty.http.HttpProtocol;

/**
 * Enables HTTP/2 cleartext (h2c) on the embedded Reactor Netty server.
 *
 * <p>Required on Cloud Run when the service is deployed with {@code --use-http2}.
 * TLS is terminated by Google's front end, so the container must accept
 * h2c on $PORT. Without this, Spring Boot's {@code server.http2.enabled=true}
 * is a no-op (it only takes effect when SSL is configured), and Cloud Run's
 * GFE → container hop stays on HTTP/1.1, capping request bodies at 32 MiB
 * with a {@code 413 Content Too Large}.</p>
 */
@Configuration
@Profile("prod")
public class NettyH2cConfig {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> h2cCustomizer() {
        return factory -> factory.addServerCustomizers(
                httpServer -> httpServer.protocol(HttpProtocol.HTTP11, HttpProtocol.H2C));
    }
}

