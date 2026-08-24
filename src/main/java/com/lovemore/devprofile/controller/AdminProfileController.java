package com.lovemore.devprofile.controller;

import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.repository.ProfileRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Everything here is scoped to "whoever is currently logged in" via the
 * Principal Spring injects automatically once SecurityConfig requires
 * authentication on /admin/**. There is no "which profile am I editing"
 * parameter anywhere - that's deliberate. It means there is no URL a user
 * could edit to reach someone else's profile through this controller.
 */
@Controller
@RequestMapping("/admin")
public class AdminProfileController {

    private final ProfileRepository profileRepository;

    public AdminProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public String dashboard(Principal principal, Model model) {
        Profile profile = profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile - registration is broken"));
        model.addAttribute("profile", profile);
        return "admin/dashboard";
    }

    @PostMapping("/profile")
    public String updateProfile(Principal principal, @Valid @ModelAttribute("profile") Profile submitted,
                                 BindingResult result, RedirectAttributes redirectAttributes) {
        Profile existing = profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));

        if (result.hasErrors()) {
            return "admin/dashboard";
        }

        // Copy only the editable scalar fields onto the managed entity - never
        // save `submitted` directly. Its `projects` collection is empty (the
        // form doesn't send it), and with orphanRemoval=true on Profile.projects,
        // saving `submitted` as-is would silently delete every project this
        // user owns. This exact bug bit an earlier version of this app.
        existing.setFullName(submitted.getFullName());
        existing.setHeadline(submitted.getHeadline());
        existing.setSummary(submitted.getSummary());
        existing.setContactEmail(submitted.getContactEmail());
        existing.setContactPhone(submitted.getContactPhone());
        existing.setLocation(submitted.getLocation());
        existing.setCvUrl(submitted.getCvUrl());
        profileRepository.save(existing);

        redirectAttributes.addFlashAttribute("success", "Profile updated.");
        return "redirect:/admin";
    }
}
