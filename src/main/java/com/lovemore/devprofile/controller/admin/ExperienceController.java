package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.Experience;
import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.repository.ExperienceRepository;
import com.lovemore.devprofile.repository.ProfileRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/admin/experience")
public class ExperienceController {

    private final ExperienceRepository experienceRepository;
    private final ProfileRepository profileRepository;

    public ExperienceController(ExperienceRepository experienceRepository, ProfileRepository profileRepository) {
        this.experienceRepository = experienceRepository;
        this.profileRepository = profileRepository;
    }

    private Profile currentProfile(Principal principal) {
        return profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));
    }

    private Experience ownedOrThrow(Long id, Principal principal) {
        Experience item = experienceRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getProfile().getOwner().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your experience entry");
        }
        return item;
    }

    /**
     * Runs before every handler in this controller and feeds the sidebar
     * fragment what it needs: which link to highlight, and the username
     * for the "View public page" link. Keeps that logic out of every
     * individual GET method.
     */
    @ModelAttribute("activePage")
    public String activePage() {
        return "experience";
    }

    @ModelAttribute("profileUsername")
    public String profileUsername(java.security.Principal principal) {
        return principal.getName();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("items", experienceRepository.findByProfileIdOrderById(currentProfile(principal).getId()));
        return "admin/experience/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("experience", new Experience());
        return "admin/experience/form";
    }

    @PostMapping
    public String create(Principal principal, @Valid @ModelAttribute("experience") Experience experience,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "admin/experience/form";
        experience.setProfile(currentProfile(principal));
        experienceRepository.save(experience);
        redirectAttributes.addFlashAttribute("success", "Experience added.");
        return "redirect:/admin/experience";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("experience", ownedOrThrow(id, principal));
        return "admin/experience/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Principal principal, @Valid @ModelAttribute("experience") Experience submitted,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "admin/experience/form";
        Experience existing = ownedOrThrow(id, principal);
        existing.setJobTitle(submitted.getJobTitle());
        existing.setCompany(submitted.getCompany());
        existing.setLocation(submitted.getLocation());
        existing.setStartDate(submitted.getStartDate());
        existing.setEndDate(submitted.getEndDate());
        existing.setCurrent(submitted.isCurrent());
        existing.setDescription(submitted.getDescription());
        experienceRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Experience updated.");
        return "redirect:/admin/experience";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        experienceRepository.delete(ownedOrThrow(id, principal));
        redirectAttributes.addFlashAttribute("success", "Experience removed.");
        return "redirect:/admin/experience";
    }
}
