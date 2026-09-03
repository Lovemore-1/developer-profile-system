package com.lovemore.devprofile.controller;

import com.lovemore.devprofile.dto.RegistrationForm;
import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.UserRepository;
import com.lovemore.devprofile.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Anyone can reach /register (see SecurityConfig). Signing up does three
 * things in sequence: creates the User (login identity), creates their
 * empty starter Profile, and emails them a 6-digit code they have to enter
 * on /verify-otp before the account can log in - see OtpService and
 * SecurityConfig's userDetailsService (that's what actually enforces it).
 */
@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public RegistrationController(UserRepository userRepository, ProfileRepository profileRepository,
                                   PasswordEncoder passwordEncoder, OtpService otpService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                            BindingResult result, Model model) {
        // Bean Validation (@NotBlank, @Email, @Size on RegistrationForm) has
        // already run by the time we get here. Comparing the two password
        // fields against each other is a cross-field check, which plain
        // annotations on a single field can't express - so it's done here
        // by hand instead, and rejectValue attaches the error to the
        // confirmPassword field specifically, the same way a failed @Email
        // check attaches to username.
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }

        if (result.hasErrors()) {
            return "register";
        }

        if (userRepository.existsByUsername(form.getUsername())) {
            model.addAttribute("usernameTaken", true);
            return "register";
        }

        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword())); // hash - never store raw
        user.setRole("USER");
        // Every other path that creates a User (GoogleOidcUserService,
        // AdminAccountSeeder) leaves this at its default of true. This is
        // the one place it's explicitly false, because a plain email/
        // password signup is the one case where we actually need proof the
        // person typed a real, reachable email address.
        user.setEmailVerified(false);
        userRepository.save(user);

        // Starter profile, placeholder text so it's editable from /admin immediately.
        Profile profile = new Profile();
        profile.setOwner(user);
        profile.setFullName(form.getUsername());
        profile.setHeadline("Add your headline in /admin");
        profile.setContactEmail(form.getUsername());
        profileRepository.save(profile);

        otpService.issueNewCode(user);

        String encodedEmail = URLEncoder.encode(form.getUsername(), StandardCharsets.UTF_8);
        return "redirect:/verify-otp?email=" + encodedEmail;
    }
}
