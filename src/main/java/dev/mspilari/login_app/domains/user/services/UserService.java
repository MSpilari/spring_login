package dev.mspilari.login_app.domains.user.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.mspilari.login_app.domains.email.services.EmailService;
import dev.mspilari.login_app.domains.user.entity.UserEntity;
import dev.mspilari.login_app.domains.user.enums.Role;
import dev.mspilari.login_app.domains.user.repositories.UserRepository;
import dev.mspilari.login_app.utils.JwtActions;

@Service
public class UserService {

    @Value("${token.expiration.seconds:300}")
    private Long tokenExpirationSeconds;

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    private final JwtActions jwtActions;

    private final EmailService emailService;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtActions jwtActions,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtActions = jwtActions;
        this.emailService = emailService;
    }

    private Optional<UserEntity> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private boolean verifyPassword(String rawPassword, String encodedPassword) {

        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private void sendPasswordResetEmail(String email, String token) {
        String subject = "Password Reset Request";
        String resetUrl = "https://seusite.com/reset?token=" + token;
        String body = "Click the link to reset your password: " + resetUrl;

        // Implemente o serviço de e-mail conforme necessário
        emailService.sendEmail(email, subject, body);
    }

    public void createUser(String email, String password) {

        if (findUserByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists !");
        }

        var encodedPassword = passwordEncoder.encode(password);

        var newUser = new UserEntity(email, encodedPassword, Role.CLIENT);

        userRepository.save(newUser);
    }

    public String login(String email, String password) {
        var user = findUserByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid login credentials"));

        if (!verifyPassword(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid login credentials");
        }

        return jwtActions.jwtCreate(user.getEmail(), user.getRole().toString());

    }

    public void redeemPassword(String email) {
        var user = findUserByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid email"));

        var token = UUID.randomUUID().toString();

        user.withResetToken(token, Instant.now().plusSeconds(this.tokenExpirationSeconds));

        userRepository.save(user);

        sendPasswordResetEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String password) {
        var user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User not found"));

        if (user.getResetTokenExpiration().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        user.setPassword(passwordEncoder.encode(password));

        user.withResetToken(null, null);

        userRepository.save(user);
    }

}
