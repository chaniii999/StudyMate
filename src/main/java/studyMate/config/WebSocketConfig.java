package studyMate.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import studyMate.interceptor.JwtHandshakeInterceptor;
import studyMate.service.JwtTokenProvider;
import studyMate.repository.UserRepository;

@Configuration
@EnableWebSocketMessageBroker  // STOMP 사용 설정
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-timer")
                .addInterceptors(new JwtHandshakeInterceptor(jwtTokenProvider, userRepository))
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 topic 접두사
        registry.enableSimpleBroker("/topic");
        // 클라이언트가 서버로 보낼 메시지 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }
}
