package ru.mail.park;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.mail.park.models.User;
import ru.mail.park.models.UserRequest;
import ru.mail.park.requests.SettingsRequest;
import ru.mail.park.services.UserService;

import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;



@RestController
@CrossOrigin(origins = {"http://bacterio.herokuapp.com",
        "https://bacterio.herokuapp.com",
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://moonfaces.herokuapp.com",
        "https://moonfaces.herokuapp.com"})
public class UserController {

    private static final FailOrSuccessResponse OK_RESPONSE = new FailOrSuccessResponse(false, null);

    private final UserService userService;

    private static final int SCOREBOARD_LIMIT = 5;


    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(path = "/restapi/signup")
    public ResponseEntity<FailOrSuccessResponse> signUp(@RequestBody User body) {
        final String login = body.getLogin();
        final String email = body.getEmail();

        if (StringUtils.isEmpty(login)
                || StringUtils.isEmpty(email)
                || !body.hasPassword()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new FailOrSuccessResponse(true, "Empty fields!"));
        }
        body.evaluateHash();

        final User checkIfExist = userService.getUser(login);

        if (checkIfExist != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new FailOrSuccessResponse(true, "User already signed up!"));

        }

        userService.addUser(body);
        return ResponseEntity.ok(OK_RESPONSE);
    }

    @PostMapping(path = "/restapi/signin")
    public ResponseEntity<?> signIn(@RequestBody UserRequest body, HttpSession httpSession) {

        final String login = body.getLogin();
        final String password = body.getPassword();

        if (StringUtils.isEmpty(login) || StringUtils.isEmpty(password)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new FailOrSuccessResponse(true, "Empty fields!"));
        }

        final Integer checkIfSignedIn = (Integer) httpSession.getAttribute("id");

        if (checkIfSignedIn != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new FailOrSuccessResponse(true, "User already signed in!"));
        }

        final User registeredUser = userService.getUser(login);

        if (registeredUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new FailOrSuccessResponse(true, "This user is not signed up!"));
        }

        if (!PasswordHandler.passwordEncoder().matches(password, registeredUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new FailOrSuccessResponse(true, "Wrong password!"));
        }

        httpSession.setAttribute("id", registeredUser.getId());
        return ResponseEntity.ok(new UserResponse(registeredUser));
    }

    @PostMapping(path = "/restapi/logout")
    public ResponseEntity<FailOrSuccessResponse> logOut(HttpSession httpSession) {
        final Integer checkIfSignedIn = (Integer) httpSession.getAttribute("id");

        if (checkIfSignedIn == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new FailOrSuccessResponse(true, "You're not sighed in!"));
        }

        httpSession.invalidate();
        return ResponseEntity.ok(OK_RESPONSE);
    }

    @GetMapping(path = "/restapi/current")
    public ResponseEntity<?> currentUser(HttpSession httpSession) {
        final Integer currentUserId = (Integer) httpSession.getAttribute("id");

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new FailOrSuccessResponse(true, "Nobody is signed in!"));
        }

        final User currentUser = userService.getUser(currentUserId);
        if (currentUser != null) {
            return ResponseEntity.ok(new UserResponse(currentUser));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new FailOrSuccessResponse(true, "User is not signed up! "));
        }
    }

    @PostMapping(path = "/restapi/settings")
    public ResponseEntity<?> changeUser(@RequestBody SettingsRequest body, HttpSession httpSession) {
        final String login = body.getLogin();
        final String email = body.getEmail();
        final String password = body.getPassword();

        final Integer currentUserId = (Integer) httpSession.getAttribute("id");

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new FailOrSuccessResponse(true, "You're not signed in!"));
        }

        if (StringUtils.isEmpty(login)
                && StringUtils.isEmpty(email)
                && StringUtils.isEmpty(password)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new FailOrSuccessResponse(true, "All fields are empty!"));
        }

        final User currentUser = userService.getUser(currentUserId);

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new FailOrSuccessResponse(true, "This user is not signed up!"));
        }

        if (!StringUtils.isEmpty(login)) {
            userService.changeLogin(currentUserId, login);
            currentUser.setEmail(login);
        }
        if (!StringUtils.isEmpty(password)) {
            userService.changePassword(currentUserId, password);
            currentUser.setPassword(password);
        }
        if (!StringUtils.isEmpty(password)) {
            userService.changeEmail(currentUserId, email);
            currentUser.setEmail(email);
        }

        return ResponseEntity.ok(new UserResponse(currentUser));
    }

    @GetMapping(path = "/restapi/scoreboard")
    public ResponseEntity<?> topPlayers(Integer page) {
        final Integer count = userService.countUsers();
        Integer pages = 0;
        if (count % SCOREBOARD_LIMIT == 0) {
            pages = count / SCOREBOARD_LIMIT;
        } else {
            pages = count / SCOREBOARD_LIMIT + 1;
        }

        final List<User> topPlayers = userService.getTopPlayers(SCOREBOARD_LIMIT, page * SCOREBOARD_LIMIT);
        if (!topPlayers.isEmpty()) {
            return ResponseEntity.ok(new LeaderboardResponse(topPlayers, pages));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new FailOrSuccessResponse(true, "Users list is empty! "));
        }
    }

    private static final class FailOrSuccessResponse {
        private final Boolean error;
        private final String description;

        private FailOrSuccessResponse(@NotNull Boolean error, @Nullable String description) {
            this.error = error;
            this.description = description;
        }

        @SuppressWarnings("unused")
        public Boolean getError() {
            return error;
        }

        @SuppressWarnings("unused")
        @Nullable
        public String getDescription() {
            return description;
        }
    }

    private static final class UserResponse {
        private final String login;
        private final String email;
        private final Integer score;

        private UserResponse(@NotNull User user) {
            this.login = user.getLogin();
            this.email = user.getEmail();
            this.score = user.getScore();
        }

        @SuppressWarnings("unused")
        public String getLogin() {
            return login;
        }

        @SuppressWarnings("unused")
        public String getEmail() {
            return email;
        }

        @SuppressWarnings("unused")
        public Integer getScore() {
            return score;
        }
    }

    public static final class LeaderboardResponse {
        private final List<UserResponse> users;

        private final Integer pages;

        @JsonCreator
        public LeaderboardResponse(List<User> users, Integer pages) {
            this.pages = pages;
            this.users = users
                    .stream()
                    .map(UserResponse::new)
                    .collect(Collectors.toList());
        }

        @SuppressWarnings("unused")
        public List<UserResponse> getUsers() {
            return users;
        }

        @SuppressWarnings("unused")
        public Integer getPages() {
            return pages;
        }
    }
}