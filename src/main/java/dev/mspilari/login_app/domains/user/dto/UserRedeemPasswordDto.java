package dev.mspilari.login_app.domains.user.dto;

import jakarta.validation.constraints.Email;

public record UserRedeemPasswordDto(@Email String email) {

}
