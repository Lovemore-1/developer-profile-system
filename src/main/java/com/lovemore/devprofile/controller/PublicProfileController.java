package com.lovemore.devprofile.controller;

import com.lovemore.devprofile.repository.ProfileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * The public, no-login-required side of the system. /u/{username} looks up
 * that person's Profile and renders it read-only, live from the database -
 * this is what makes "if I share the link it shows my profile, not yours"
 * true: the URL itself carries whose data to show.
 */
@Controller
public class PublicProfileController {

    private final ProfileRepository profileRepository;

    public PublicProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping("/u/{username}")
    public String viewProfile(@PathVariable String username, Model model) {
        var profile = profileRepository.findByOwnerUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile for " + username));
        model.addAttribute("profile", profile);
        return "public/profile";
    }

    /**
     * Real DB-driven CV, not a static file. Same lookup as the profile page,
     * different template - one that's laid out for printing. The "Download
     * CV" flow is just this page plus the browser's own print-to-PDF, so
     * whatever is saved is always the current database content.
     */
    @GetMapping("/u/{username}/cv")
    public String viewCv(@PathVariable String username, Model model) {
        var profile = profileRepository.findByOwnerUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile for " + username));
        model.addAttribute("profile", profile);
        return "public/cv";
    }
}
