package ru.mail.park.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.mail.park.models.User;
import ru.mail.park.services.UserService;

import javax.naming.AuthenticationException;
import java.io.IOException;

public class GameSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameSocketHandler.class);

    private final UserService userService;
    private final MessageHandlerContainer messageHandlerContainer;
    private final RemotePointService remotePointService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public GameSocketHandler(MessageHandlerContainer messageHandlerContainer,
                             UserService userService,
                             RemotePointService remotePointService) {
        this.messageHandlerContainer = messageHandlerContainer;
        this.userService = userService;
        this.remotePointService = remotePointService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws AuthenticationException {
        final Integer id = (Integer) webSocketSession.getAttributes().get("id");
        if (id == null ||userService.getUser(id) == null) {
            throw new AuthenticationException("Only authenticated users allowed to play a game");
        }
        remotePointService.registerUser(id, webSocketSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws AuthenticationException {
        final Integer id = (Integer) webSocketSession.getAttributes().get("id");
        final User user;
        if (id == null || (user = userService.getUser(id)) == null) {
            throw new AuthenticationException("Only authenticated users allowed to play a game");
        }
        handleMessage(user, message);
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private void handleMessage(User userProfile, TextMessage text) {

        final Message message;
        try {
            message = objectMapper.readValue(text.getPayload(), Message.class);
        } catch (IOException ex) {
            LOGGER.error("wrong json format at ping response", ex);
            return;
        }
        try {
            messageHandlerContainer.handle(message, userProfile.getId());
        } catch (HandleException e) {
            LOGGER.error("Can't handle message of type " + message.getType() + " with content: " + message.getContent(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {
        LOGGER.warn("Websocket transport problem", throwable);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {
        final Integer userId = (Integer) webSocketSession.getAttributes().get("userId");
        if (userId == null) {
            LOGGER.warn("User disconnected but his session was not found (closeStatus=" + closeStatus + ')');
            return;
        }
        remotePointService.removeUser(userId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
