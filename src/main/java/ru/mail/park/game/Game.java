package ru.mail.park.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.park.websocket.NetUser;

public final class Game {

    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    private static int id = 0;

    private NetUser[] users = new NetUser[2];

    private Game(NetUser user1, NetUser user2) {
        users[0] = user1;
        users[1] = user2;
    }

    public static synchronized void createGame(NetUser user1, NetUser user2) {
        final Game room = new Game(user1, user2);
        new Thread(String.format("Game #%d main thread", id++)) {
            @Override
            public void run() {
                room.start();
            }
        }.start();
    }

    private void start() {

    }
}
