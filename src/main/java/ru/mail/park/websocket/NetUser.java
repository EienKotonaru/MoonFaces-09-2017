package ru.mail.park.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class NetUser implements Consumer<String> {
    private static final Integer CAPACITY = 500;
    private final String login;
    private List<WebSocketSession> sessions = Collections.synchronizedList(new ArrayList<>());
    private UserStatus status = UserStatus.WAITING;
    private Queue<String> inputMessages = new ArrayBlockingQueue<>(CAPACITY);
    private Queue<String> outputMessages = new ArrayBlockingQueue<>(CAPACITY);
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public NetUser(WebSocketSession session) {
        login = (String) session.getAttributes().get("login");
        addConnection(session);
        new Thread(String.format("User(%s) messages sender", login)) {
            @Override
            public void run() {
                while (true) {
                    lock.lock();
                    try {
                        condition.await();
                        trySend();
                    } catch (InterruptedException e) {
                        return;
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }.start();
    }

    public String getLogin() {
        return login;
    }

    public void addConnection(WebSocketSession session) {
        sessions.add(session);
        GameSocketHandler.addHandleTextMessageListener(this, session);
        lock.lock();
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    private void trySend() {
        while (!outputMessages.isEmpty() && !sessions.isEmpty()) {
            final String message = outputMessages.element();
            boolean send = false;
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(message));
                    send = true;
                } catch (IOException e) {
                    sessions.remove(session);
                }
            }
            if (send) {
                outputMessages.remove();
            }
        }
        if (sessions.isEmpty()) {
            status = UserStatus.DISC;
        }
    }

    @Override
    public void accept(String str) {
        inputMessages.add(str);
    }

    @SuppressWarnings("unused")
    public void send(String str) {
        outputMessages.add(str);
        lock.lock();
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unused")
    public String receive() {
        return inputMessages.remove();
    }

    @SuppressWarnings("unused")
    public UserStatus getStatus() {
        return status;
    }

    @SuppressWarnings("unused")
    private enum UserStatus {
        WAITING, GAME, DISC
    }
}
