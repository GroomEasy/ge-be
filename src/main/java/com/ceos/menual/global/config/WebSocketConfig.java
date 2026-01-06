package com.ceos.menual.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 소켓 연결 (Handshake) 엔드포인트
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "https://ge-fe-pi.vercel.app",
                        "https://www.menual.site"
                )
                .addInterceptors(authHandshakeInterceptor) // 핸드셰이크 인터셉터 등록
                .withSockJS(); // SockJS 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 구독 (Subscribe) 경로 prefix
        registry.enableSimpleBroker("/sub");

        // 메시지 전송 (Publish) 경로 prefix
        registry.setApplicationDestinationPrefixes("/pub");
    }

}
