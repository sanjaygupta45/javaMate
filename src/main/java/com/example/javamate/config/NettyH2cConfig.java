package com.example.javamate.config;


import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.netty.http.HttpProtocol;


@Configuration
@Profile("prod")
public class NettyH2cConfig {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> h2cCustomizer() {
        return factory -> factory.addServerCustomizers(
                httpServer -> httpServer.protocol(HttpProtocol.HTTP11, HttpProtocol.H2C));
    }
}

