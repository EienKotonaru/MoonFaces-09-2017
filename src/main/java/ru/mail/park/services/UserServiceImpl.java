package ru.mail.park.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import ru.mail.park.PasswordHandler;
import ru.mail.park.models.User;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final JdbcTemplate template;

    @Autowired
    public UserServiceImpl(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void addUser(@NotNull User user) {
        try {
            template.update("INSERT INTO users(login, email, password, score) VALUES(?, ?, ?, ?)",
                    user.getLogin(), user.getEmail(), user.getPassword(), user.getScore());
        } catch (DuplicateKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public User getUser(@NotNull String login) {
        try {
            return template.queryForObject("SELECT id, login, email, password, score FROM users WHERE login=?", USER_MAPPER, login);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    @Nullable
    public User getUser(@NotNull Integer id) {
        try {
            return template.queryForObject("SELECT id, login, email, password, score FROM users WHERE id=?", USER_MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void changeLogin(Integer id, String newLogin) {
        template.update("UPDATE users SET login=? WHERE id=?", newLogin, id);
    }

    @Override
    public void changePassword(Integer id, String newPassword) {
        final String newPasswordHash = PasswordHandler.passwordEncoder().encode(newPassword);
        template.update("UPDATE users SET password=? WHERE id=?", newPasswordHash, id);
    }

    @Override
    public void changeEmail(Integer id, String newEmail) {
        template.update("UPDATE users SET email=? WHERE id=?", newEmail, id);
    }

    @Override
    public void changeScore(Integer id, Integer newScore) {
        template.update("UPDATE users SET score=? WHERE id=?", newScore, id);
    }

    @Override
    public List<User> getTopPlayers(int limit, int offset) {
        return template.query(
                "SELECT id, login, email, password, score FROM users ORDER BY score DESC, login ASC LIMIT ? OFFSET ?",
                USER_MAPPER, limit, offset
        );
    }

    @Override
    public Integer countUsers() {
        return template.queryForObject("SELECT COUNT(*) FROM users", INTEGER_MAPPER);
    }

    static final int USER_ID = 1;
    static final int LOGIN = 2;
    static final int EMAIL = 3;
    static final int PASSWORD = 4;
    static final int SCORE = 5;
    private static final RowMapper<User> USER_MAPPER =
            (res, rowNum) -> new User(res.getInt(USER_ID),
                                      res.getString(LOGIN),
                                      res.getString(EMAIL),
                                      res.getString(PASSWORD),
                                      res.getInt(SCORE));

    private static final RowMapper<Integer> INTEGER_MAPPER =
            (res, rowNum) -> res.getInt(1);
}

