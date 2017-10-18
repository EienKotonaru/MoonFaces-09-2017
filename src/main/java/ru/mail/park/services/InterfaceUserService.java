package ru.mail.park.services;

import org.jetbrains.annotations.NotNull;
import ru.mail.park.exceptions.UserAlreadyExists;
import ru.mail.park.models.User;


public interface InterfaceUserService {
    void addUser(@NotNull User user) throws UserAlreadyExists;

    User getUser(@NotNull String login);

    void changePassword(String login, String newPassword);

    void changeEmail(String login, String email);
}
