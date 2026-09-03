package com.lovemore.devprofile.service;

import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;

    private final UserRepository userRepository;
    private final String resendApiKey;
    private final HttpClient httpClient;

    public OtpService(UserRepository userRepository, @Value("${resend.api-key}") String resendApiKey) {
        this.userRepository = userRepository;
        this.resendApiKey = resendApiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void issueNewCode(User user) {
        String code = generateCode();
        user.setOtpCode(code);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        userRepository.save(user);

        System.out.println("[OTP] Code for " + user.getUsername() + " is " + code
                + " (expires in " + EXPIRY_MINUTES + " minutes)");

        try {
            String text = "Your verification code is " + code + ".\n\n"
                    + "It expires in " + EXPIRY_MINUTES + " minutes. If you didn't request this, "
                    + "you can ignore this email.";

            String body = "{"
                    + "\"from\":\"Developer Profile System <onboarding@resend.dev>\","
                    + "\"to\":[\"" + jsonEscape(user.getUsername()) + "\"],"
                    + "\"subject\":\"Your Developer Profile System verification code\","
                    + "\"text\":\"" + jsonEscape(text) + "\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("[OTP] Resend rejected the email to " + user.getUsername()
                        + " - HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            System.out.println("[OTP] Could not email the code to " + user.getUsername()
                    + " - " + e.getMessage());
        }
    }

    public boolean verify(User user, String submittedCode) {
        if (user.getOtpCode() == null || user.getOtpExpiresAt() == null) {
            return false;
        }
        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (submittedCode == null || !user.getOtpCode().equals(submittedCode.trim())) {
            return false;
        }

        user.setEmailVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    private String generateCode() {
        int number = RANDOM.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", number);
    }

    private String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
