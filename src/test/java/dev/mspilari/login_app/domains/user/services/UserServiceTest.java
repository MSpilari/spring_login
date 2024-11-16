package dev.mspilari.login_app.domains.user.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import dev.mspilari.login_app.domains.email.services.EmailService;
import dev.mspilari.login_app.domains.user.entity.UserEntity;
import dev.mspilari.login_app.domains.user.enums.Role;
import dev.mspilari.login_app.domains.user.repositories.UserRepository;
import dev.mspilari.login_app.utils.JwtActions;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtActions jwtActions;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Nested
    class CreateUser {
        @Test
        void shouldCreateUserWhenEmailIsNew() {
            var email = "test@email.com";
            var rawPassword = "12345";
            var encodedPassword = "12345_encoded";

            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            userService.createUser(email, rawPassword);

            verify(userRepository, times(1)).save(argThat(user -> user.getEmail().equals(email) &&
                    user.getPassword().equals(encodedPassword) &&
                    user.getRole() == Role.CLIENT));
        }

        @Test
        void shouldNotCreateUserWhenEmailAlreadyExists() {
            var email = "test@email.com";
            var rawPassword = "12345";
            var existingUser = new UserEntity(email, "existing_password", Role.CLIENT);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.createUser(email, rawPassword));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

            assertEquals("Email already exists !", exception.getReason());

            verify(userRepository, times(0)).save(any(UserEntity.class));
        }
    }

    @Nested
    class UserLogin {
        @Test
        void shouldBeAbleToLoginSuccessfully() {
            var email = "test@email.com";
            var rawPassword = "12345";
            var userEntity = new UserEntity(email, rawPassword, Role.CLIENT);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

            when(passwordEncoder.matches(rawPassword, rawPassword)).thenReturn(true);

            when(jwtActions.jwtCreate(email, Role.CLIENT.toString())).thenReturn("Fake Token");

            var result = userService.login(email, rawPassword);

            assertEquals("Fake Token", result);
        }

        @Test
        void shouldNotBeAbleToLoginDueToEmail() {
            var email = "test@email.com";
            var rawPassword = "12345";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.login(email, rawPassword));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Invalid login credentials", exception.getReason());

        }

        @Test
        void shouldNotBeAbleToLoginDueToPassword() {
            var email = "test@email.com";
            var rawPassword = "12345";
            var userEntity = new UserEntity(email, rawPassword, Role.CLIENT);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(rawPassword, rawPassword)).thenReturn(false);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.login(email, rawPassword));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Invalid login credentials", exception.getReason());

        }
    }

    @Nested
    class RedeemPassword {
        @Test
        void shouldBeAbleToRedeemSuccessfully() throws Exception {
            var email = "test@email.com";
            var rawPassword = "12345";
            var userEntity = new UserEntity(email, rawPassword, Role.CLIENT);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

            Field tokenExpirationField = UserService.class.getDeclaredField("tokenExpirationSeconds");
            tokenExpirationField.setAccessible(true);
            tokenExpirationField.set(userService, 300L);

            userService.redeemPassword(email);

            verify(userRepository, times(1)).save(argThat(user -> user.getResetToken() != null &&
                    user.getResetTokenExpiration().isAfter(Instant.now())));
            verify(emailService, times(1)).sendEmail(
                    argThat(emailArg -> emailArg.equals(email)),
                    any(String.class),
                    any(String.class));

        }

        @Test
        void shouldNotBeAbleToRedeemDueToEmail() {
            var email = "test@email.com";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.redeemPassword(email));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Invalid email", exception.getReason());

        }
    }

    @Nested
    class ResetPassword {
        @Test
        void shouldNotBeAbleToResetDueToToken() {
            var token = "fake reset token";

            when(userRepository.findByResetToken(token)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.resetPassword(token, "password"));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("User not found", exception.getReason());

        }

        @Test
        void shouldNotBeAbleToResetDueToTokenExpired() {
            var token = "fake token";
            var user = new UserEntity("test@email.com", "12345", Role.CLIENT);

            when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));

            var expiredTime = Instant.now().minusSeconds(3600);
            user.withResetToken(token, expiredTime);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userService.resetPassword(token, "12345"));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Token expired", exception.getReason());

            verify(userRepository, times(0)).save(any(UserEntity.class));

        }

        @Test
        void shouldBeAbleToResetPassword() {
            var token = "fake token";
            var user = new UserEntity("test@email.com", "12345", Role.CLIENT);

            when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword")).thenReturn("newPassword");

            var expiredTime = Instant.now().plusSeconds(3600);
            user.withResetToken(token, expiredTime);

            userService.resetPassword(token, "newPassword");

            verify(userRepository, times(1)).save(argThat(savedUser -> savedUser.getPassword() != null &&
                    !savedUser.getPassword().equals("12345") &&
                    savedUser.getResetToken() == null &&
                    savedUser.getResetTokenExpiration() == null));

        }
    }

}
