package dev.mspilari.login_app.domains.user.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import dev.mspilari.login_app.domains.user.dto.UserDto;
import dev.mspilari.login_app.domains.user.dto.UserRedeemPasswordDto;
import dev.mspilari.login_app.domains.user.dto.UserResetPasswordDto;
import dev.mspilari.login_app.domains.user.services.UserService;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Nested
    class LoginTests {

        @Test
        void shouldLoginSuccessfully() {
            var userDto = new UserDto("test@email.com", "12345");

            when(userService.login(userDto.email(), userDto.password())).thenReturn("FakeToken");

            ResponseEntity<Map<String, String>> response = userController.login(userDto);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("FakeToken", response.getBody().get("token"));
        }

        @Test
        void shouldFailLoginWithInvalidCredentials() {
            var userDto = new UserDto("test@email.com", "wrongPassword");

            when(userService.login(userDto.email(), userDto.password()))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid login credentials"));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userController.login(userDto));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Invalid login credentials", exception.getReason());
        }
    }

    @Nested
    class RegisterTests {
        @Test
        void shouldRegisterUserSuccessfully() {
            var userDto = new UserDto("test@email.com", "12345");

            ResponseEntity<Map<String, String>> response = userController.register(userDto);

            verify(userService, times(1)).createUser(userDto.email(), userDto.password());
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("User created successfully", response.getBody().get("message"));
        }

        @Test
        void shouldFailRegisterIfEmailAlreadyExists() {
            var userDto = new UserDto("test@email.com", "12345");

            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists"))
                    .when(userService).createUser(userDto.email(), userDto.password());

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userController.register(userDto));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Email already exists", exception.getReason());
        }

    }

    @Nested
    class RedeemPassword {
        @Test
        void shouldRedeemPasswordSuccessfully() {
            // Arrange
            String email = "test@example.com";
            UserRedeemPasswordDto userDto = new UserRedeemPasswordDto(email);
            doNothing().when(userService).redeemPassword(email);

            // Act
            ResponseEntity<Map<String, String>> response = userController.redeemPassword(userDto);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Send the redeem password link to your email", response.getBody().get("message"));
            Mockito.verify(userService, Mockito.times(1)).redeemPassword(email);
        }

        @Test
        void shouldNotRedeemPasswordDueToInvalidEmail() {
            String email = "invalid-email";
            UserRedeemPasswordDto userDto = new UserRedeemPasswordDto(email);

            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email"))
                    .when(userService).redeemPassword(email);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userController.redeemPassword(userDto));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Invalid email", exception.getReason());

            verify(userService, times(1)).redeemPassword(email);

        }

        @Test
        void shouldNotRedeemPasswordDueToEmailNotFound() {

            String email = "notfound@example.com";
            UserRedeemPasswordDto userDto = new UserRedeemPasswordDto(email);

            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email not found"))
                    .when(userService).redeemPassword(email);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userController.redeemPassword(userDto));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Email not found", exception.getReason());

            verify(userService, times(1)).redeemPassword(email);

        }
    }

    @Nested
    class ResetPassword {

        @Test
        void shouldResetPasswordSuccessfully() {

            String token = "valid-token";
            String newPassword = "newSecurePassword123!";
            UserResetPasswordDto userDto = new UserResetPasswordDto(token, newPassword);

            ResponseEntity<Map<String, String>> response = userController.resetPassword(userDto);

            verify(userService, times(1)).resetPassword(token, newPassword);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Credentials updated", response.getBody().get("message"));
        }

        @Test
        void shouldFailResetPasswordDueToInvalidToken() {

            String token = "invalid-token";
            String newPassword = "newSecurePassword123!";
            UserResetPasswordDto userDto = new UserResetPasswordDto(token, newPassword);

            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"))
                    .when(userService).resetPassword(token, newPassword);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userController.resetPassword(userDto));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Invalid token", exception.getReason());

            verify(userService, times(1)).resetPassword(token, newPassword);
        }

        @Test
        void shouldFailResetPasswordDueToExpiredToken() {

            String token = "expired-token";
            String newPassword = "newSecurePassword123!";
            UserResetPasswordDto userDto = new UserResetPasswordDto(token, newPassword);

            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired"))
                    .when(userService).resetPassword(token, newPassword);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> userController.resetPassword(userDto));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Token expired", exception.getReason());

            verify(userService, times(1)).resetPassword(token, newPassword);
        }
    }

}
