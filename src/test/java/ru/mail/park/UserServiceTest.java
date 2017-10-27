package ru.mail.park;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mail.park.exceptions.UserAlreadyExists;
import ru.mail.park.models.User;
import ru.mail.park.services.UserServiceImpl;

import java.util.List;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class UserServiceTest extends Assert {

    @Autowired
    private JdbcTemplate template;

    @Autowired
    private UserServiceImpl userService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @After
    public void clearTestTable() {
        template.execute("TRUNCATE TABLE users");
    }

    @Test
    public void testUser() throws UserAlreadyExists {
        final User user = new User("login", "email", "password");
        userService.addUser(user);
        final User created = userService.getUser(user.getLogin());
        assertNotNull(created);
        assertEquals(user.getLogin(), created.getLogin());
        assertEquals(user.getPassword(), created.getPassword());
        assertEquals(user.getEmail(), created.getEmail());
    }

    @Test
    public void testExistingUser() throws UserAlreadyExists {
        final User user = new User("login", "email", "password");
        userService.addUser(user);
        expectedException.expect(RuntimeException.class);
        userService.addUser(user);
    }

    @Test
    public void testChangingUser() throws UserAlreadyExists{
        final User user = new User("login", "email", "password");
        userService.addUser(user);

        final User addedUser = userService.getUser(user.getLogin());
        assertNotNull(addedUser);

        final String newLogin = "new_login";
        userService.changeLogin(addedUser.getId(), newLogin);

        final String newPassword = "new_password";
        userService.changePassword(addedUser.getId(), newPassword);

        final String newEmail = "new_email";
        userService.changeEmail(addedUser.getId(), newEmail);

        final Integer newScore = 42;
        userService.changeScore(addedUser.getId(), newScore);

        final User created = userService.getUser(user.getLogin());
        if(created != null) {
            assertEquals(newLogin, created.getLogin());
            assertTrue(PasswordHandler.passwordEncoder().matches(newPassword, created.getPassword()));
            assertEquals(newEmail, created.getEmail());
            assertEquals(newScore, created.getScore());
        }
    }

    @Test
    public void testScoreboard() {
        final User user = new User("login", "email", "password");
        userService.addUser(user);
        final User addedUser = userService.getUser(user.getLogin());
        assertNotNull(addedUser);
        userService.changeScore(addedUser.getId(), 1);

        final User user2 = new User("login2", "email2", "password2");
        userService.addUser(user2);
        final User addedUser2 = userService.getUser(user2.getLogin());
        assertNotNull(addedUser2);
        userService.changeScore(addedUser2.getId(), 2);

        final List<User> result = userService.getTopPlayers(2,0);

        assertEquals(result.get(0).getId(), addedUser2.getId());
        assertEquals(result.get(1).getId(), addedUser.getId());

        final Integer realCount = userService.countUsers();
        final Integer expecterCount = 2;
        assertEquals(expecterCount, realCount);
    }
}
