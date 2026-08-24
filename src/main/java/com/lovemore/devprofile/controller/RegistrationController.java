package com.lovemore.devprofile.controller;

import com.lovemore.devprofile.dto.RegistrationForm;
import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Anyone can reach /register (see SecurityConfig). Signing up does two
 * things in one transaction-ish sequence: creates the User (login identity)
 * and creates their empty starter Profile in the same step, so every
 * account always has exactly one profile to edit - the rest of the app
 * never has to handle "logged in but no profile yet".
 */
@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, ProfileRepository profileRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                            BindingResult result, Model model) {
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
        userRepository.save(user);

        // Starter profile, placeholder text so it's editable from /admin immediately.
        Profile profile = new Profile();
        profile.setOwner(user);
        profile.setFullName(form.getUsername());
        profile.setHeadline("Add your headline in /admin");
        profile.setContactEmail("you@example.com");
        profileRepository.save(profile);

        return "redirect:/login?registered";
    }
}
