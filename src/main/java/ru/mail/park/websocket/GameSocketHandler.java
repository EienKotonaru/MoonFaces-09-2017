package ru.mail.park.websocket;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GameSocketHandler extends TextWebSocketHandler {

    private static final Integer TIMEOUT = 5;
    private static final Set<WebSocketSession> SESSIONS = Collections.synchronizedSet(new HashSet<WebSocketSession>());
    private static final Map<WebSocketSession, Consumer<String>> LISTENERS = Collections.synchronizedMap(new HashMap<>());
    private static final Logger LOGGER = LoggerFactory.getLogger(GameSocketHandler.class);

    static {
        new Thread(() -> {
            try {
                while (true) {
                    TimeUnit.MINUTES.sleep(TIMEOUT);
                    for (WebSocketSession wss : SESSIONS) {
                        if (!wss.isOpen()) {
                            SESSIONS.remove(wss);
                            if (LISTENERS.containsKey(wss)) {
                                LISTENERS.remove(wss);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.warn("WS session clearer is interrupted", e);
            }
        }, "WS session clearer").start();
    }

    public static void addHandleTextMessageListener(Consumer<String> listener, WebSocketSession webSocketSession) {
        LISTENERS.put(webSocketSession, listener);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SESSIONS.remove(session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOGGER.info(String.format("New WS connection %s", session.getRemoteAddress()));
        LOGGER.info(session.toString());
        LOGGER.info(session.getAttributes().get("id").toString());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        LOGGER.info(String.format("New message\n%s", message.getPayload()));
        if (LISTENERS.containsKey(session)) {
            LISTENERS.get(session).accept(message.getPayload());
        }
    }
}