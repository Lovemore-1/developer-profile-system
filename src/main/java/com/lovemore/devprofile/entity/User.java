package com.lovemore.devprofile.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER";

    // Defaults to true on purpose, both here and at the database level
    // (see columnDefinition below). This column was added after the app
    // already had real registered users in production - if it defaulted
    // to false, every existing account would suddenly be locked out of
    // login the next time the app started, with no way to get a code for
    // an account created before this feature existed. RegistrationController
    // is the ONE place that explicitly flips this to false, because that's
    // the only path where "not yet verified" is actually true.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified = true;

    // Null when there's no pending code. Only ever set by OtpService.
    private String otpCode;
    private LocalDateTime otpExpiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }
}
