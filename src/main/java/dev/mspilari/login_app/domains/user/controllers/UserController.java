package dev.mspilari.login_app.domains.user.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.mspilari.login_app.domains.user.dto.UserDto;
import dev.mspilari.login_app.domains.user.dto.UserRedeemPasswordDto;
import dev.mspilari.login_app.domains.user.dto.UserResetPasswordDto;
import dev.mspilari.login_app.domains.user.services.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("user")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody @Valid UserDto userDto) {
        var token = userService.login(userDto.email(), userDto.password());
        return ResponseEntity.ok().body(Map.of("token", token));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody @Valid UserDto userDto) {
        userService.createUser(userDto.email(), userDto.password());

        return ResponseEntity.ok().body(Map.of("message", "User created successfully"));
    }

    @PostMapping("/redeem-password")
    public ResponseEntity<Map<String, String>> redeemPassword(@RequestBody @Valid UserRedeemPasswordDto userDto) {
        userService.redeemPassword(userDto.email());

        return ResponseEntity.ok().body(Map.of("message", "Send the redeem password link to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody @Valid UserResetPasswordDto userDto) {
        userService.resetPassword(userDto.token(), userDto.password());

        return ResponseEntity.ok().body(Map.of("message", "Credentials updated"));
    }

}
