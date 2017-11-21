package ru.mail.park.websocket;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("NullableProblems")
public class Message {
    private @NotNull
    String type;
    private @NotNull String content;

    public @NotNull String getType() {
        return type;
    }
    public @NotNull String getContent() {
        return content;
    }

    public Message() {
    }

    public Message(@NotNull String type, @NotNull String content) {
        this.type = type;
        this.content = content;
    }

    public Message(@NotNull Class clazz, @NotNull String content) {
        this(clazz.getName(), content);
    }
}
