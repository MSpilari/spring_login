package dev.mspilari.login_app.domains.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UserDto(@Email @NotBlank String email, @Min(4) @NotBlank String password) {

}
