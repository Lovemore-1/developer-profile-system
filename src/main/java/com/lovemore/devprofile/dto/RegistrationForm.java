package com.lovemore.devprofile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * NOT an entity - this is a plain form-backing object, deliberately separate
 * from User. It exists so validation rules for "signing up" (min password
 * length, must be a real email, etc.) don't have to live on the User entity
 * itself, and so the raw password field passing through here never touches
 * the database directly - RegistrationController hashes it before ever
 * constructing a User.
 *
 * The field is still called "username" internally (that's what Spring
 * Security's login form and UserDetailsService expect), but @Email forces
 * whatever gets typed into it to actually look like an email address -
 * "sdf" or "admin" will now fail validation and re-show the form with an
 * error instead of creating an account.
 */
public class RegistrationForm {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Email is too long")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
