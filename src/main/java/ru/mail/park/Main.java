package ru.mail.park;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import ru.mail.park.websocket.GameSocketHandler;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(new Object[]{WebSocketConfig.class, Main.class}, args);
    }

    @Bean
    public WebSocketHandler gameWebSocketHandler() {
        return new PerConnectionWebSocketHandler(GameSocketHandler.class);
    }


}