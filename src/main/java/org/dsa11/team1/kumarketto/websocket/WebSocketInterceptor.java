package org.dsa11.team1.kumarketto.websocket;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler webSocketHandler,
                                   Map<String, Object> attributes) throws Exception {


        // HTTP 요청 정보를 Servlet 단위의 요청 정보로 변환하여
        //    현재 사용자의 세션(HttpSession)에 접근합니다.
        //    이 세션에는 사용자가 로그인할 때 저장된 정보가 담겨 있습니다.
        HttpSession httpSession = ((ServletServerHttpRequest) request)
                .getServletRequest().getSession(false);


        //  세션이 존재하고, 세션 안에 "userId"라는 속성이 저장되어 있는지 확인합니다.
        //    (이 "userId"는 WebSecurityConfig에서 로그인 성공 시 세션에 저장해 둔 값입니다.)
        if(httpSession != null && httpSession.getAttribute("userId") != null) {

            // 'attributes'는 앞으로 생성될 웹소켓 세션의 개인 사물함입니다.
            //    이 사물함에 HTTP 세션에서 꺼낸 "userId"를 그대로 복사해서 넣어줍니다.
            //    이것이 바로 '이름표'를 붙이는 과정입니다.
            attributes.put("userId", httpSession.getAttribute("userId"));
            // 신원 확인이 완료되었으니, 핸드셰이크를 계속 진행하도록 '허가'합니다.
            return true;
        }
        // return false: 세션에 userId가 없다면 (비로그인 사용자), 연결을 '거부'합니다.
        return false;

    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler webSocketHandler,
                               Exception exception) {
    }

}

//@Component
//public class WebSocketInterceptor implements ChannelInterceptor {
//
//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        // STOMP 메시지의 헤더 접근
//        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//
//        if (accessor != null) {
//            switch (accessor.getCommand()) {
//                case CONNECT:
//                    // 클라이언트에서 전송한 헤더 예: userId
//                    String userId = accessor.getFirstNativeHeader("userId");
//                    if (userId != null && !userId.isEmpty()) {
//                        accessor.getSessionAttributes().put("userId", userId);
//                    } else {
//                        // userId 없으면 연결 거부 가능
//                        throw new IllegalArgumentException("userId is required for WebSocket connection");
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
//        return message;
//    }
//}