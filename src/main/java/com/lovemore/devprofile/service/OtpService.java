package com.lovemore.devprofile.service;
 
import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
 
import java.security.SecureRandom;
import java.time.LocalDateTime;
 
/**
 * Generates, stores, emails and checks the 6-digit one-time code used to
 * prove someone actually controls the email address they registered with.
 *
 * Only the RegistrationController path (plain email/password signup) ever
 * issues a code with emailVerified starting false. Google sign-in accounts
 * never touch this class - Google has already verified that email address
 * on the app's behalf, so GoogleOidcUserService marks those accounts
 * verified immediately instead.
 */
@Service
public class OtpService {
 
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;
 
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
 
    public OtpService(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }
 
    /**
     * Generates a fresh code, saves it on the user with an expiry, and
     * emails it. Used both right after registration and from the
     * "resend code" button.
     */
    public void issueNewCode(User user) {
        String code = generateCode();
        user.setOtpCode(code);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        userRepository.save(user);
 
        // Printed here regardless of whether the email actually sends, so
        // testing and demoing never depend on SMTP delivery working in the
        // moment - the real code is always visible right here too.
        System.out.println("[OTP] Code for " + user.getUsername() + " is " + code
                + " (expires in " + EXPIRY_MINUTES + " minutes)");
 
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getUsername());
            message.setSubject("Your Developer Profile System verification code");
            message.setText("Your verification code is " + code + ".\n\n"
                    + "It expires in " + EXPIRY_MINUTES + " minutes. If you didn't request this, "
                    + "you can ignore this email.");
            mailSender.send(message);
        } catch (Exception e) {
            // A failed send should never take down registration or the
            // resend button - the code is already saved and already
            // printed above, so verification still works even if the
            // email itself never arrives. Only delivery failed, not the
            // feature - and we don't want one flaky SMTP call turning
            // into a 500 on a page a user is actively looking at.
            System.out.println("[OTP] Could not email the code to " + user.getUsername()
                    + " - " + e.getMessage());
        }
    }
 
    /**
     * Checks a submitted code against what's stored on the user. On success,
     * marks the account verified and clears the code so it can't be reused.
     * Returns false for a wrong code, an expired code, or no pending code
     * at all - the controller doesn't need to know which, so this stays a
     * single boolean instead of throwing distinct exceptions per case.
     */
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
}