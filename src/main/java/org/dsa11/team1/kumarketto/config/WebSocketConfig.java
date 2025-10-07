package org.dsa11.team1.kumarketto.config;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.websocket.WebSocketHandler;
import org.dsa11.team1.kumarketto.websocket.WebSocketInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@RequiredArgsConstructor
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final WebSocketInterceptor webSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // registry: 웹소켓 핸들러를 등록하는 등록부(registry)입니다.
        // .addHandler(webSocketHandler, "/ws/chat"):
        //   "/ws/chat" 경로로 웹소켓 요청이 오면, 'webSocketHandler'라는 객체가
        //   그 요청을 최종적으로 처리하도록 지정합니다.
        registry.addHandler(webSocketHandler, "/ws/chat")
                //   최종 처리 담당자인 webSocketHandler에게 요청이 도달하기 전에,
                //   'webSocketInterceptor'라는 문지기(interceptor)를 먼저 거치도록 설정합니다.
                .addInterceptors(webSocketInterceptor)
                // .setAllowedOrigins("*"): 모든 도메인에서의 접속을 허용합니다
                .setAllowedOrigins("*");

        registry.addHandler(webSocketHandler, "/ws/admin")
                .addInterceptors(webSocketInterceptor)
                .setAllowedOrigins("*");
    }

}

//@Configuration
//@EnableWebSocketMessageBroker // ✅ STOMP 메시징 활성화
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        // 클라이언트가 구독하는 경로: /sub/...
//        config.enableSimpleBroker("/sub");
//
//        // 클라이언트가 publish(메시지 보낼 때) 사용하는 prefix: /pub/...
//        config.setApplicationDestinationPrefixes("/pub");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        // 클라이언트가 최초 연결할 때 쓰는 WebSocket 엔드포인트
//        registry.addEndpoint("/ws/chat")
//                .setAllowedOriginPatterns("*") // CORS 허용
//                .withSockJS(); // ✅ SockJS 지원 추가
//
//        // 관리자용 엔드포인트도 필요하다면 추가 가능
//        registry.addEndpoint("/ws/admin")
//                .setAllowedOriginPatterns("*")
//                .withSockJS();
//    }
//}