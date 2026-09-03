package com.lovemore.devprofile.controller;

import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.UserRepository;
import com.lovemore.devprofile.service.OtpService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Everything about entering (or re-requesting) the 6-digit code lives here.
 * Reached two ways: straight after registration (RegistrationController
 * redirects here), and when a not-yet-verified account tries to log in
 * (SecurityConfig's LoginFailureHandler redirects here instead of showing
 * the usual "wrong password" error).
 */
@Controller
public class OtpController {

    private final UserRepository userRepository;
    private final OtpService otpService;

    public OtpController(UserRepository userRepository, OtpService otpService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    @GetMapping("/verify-otp")
    public String showForm(@RequestParam String email,
                            @RequestParam(required = false) boolean unverified,
                            Model model) {
        model.addAttribute("email", email);
        model.addAttribute("unverified", unverified);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verify(@RequestParam String email, @RequestParam String code, Model model) {
        User user = userRepository.findByUsername(email).orElse(null);

        if (user == null || !otpService.verify(user, code)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "That code is incorrect or has expired. Request a new one below.");
            return "verify-otp";
        }

        return "redirect:/login?verified";
    }

    @PostMapping("/verify-otp/resend")
    public String resend(@RequestParam String email, Model model) {
        userRepository.findByUsername(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                otpService.issueNewCode(user);
            }
        });
        model.addAttribute("email", email);
        model.addAttribute("resent", true);
        return "verify-otp";
    }
}
