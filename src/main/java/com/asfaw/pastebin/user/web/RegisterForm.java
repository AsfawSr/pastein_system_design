package com.asfaw.pastebin.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "[a-zA-Z0-9_]{3,50}", message = "3-50 letters, digits or underscores")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String password;

    private String confirmPassword;
}
