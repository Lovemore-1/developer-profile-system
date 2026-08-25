package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.Education;
import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.repository.EducationRepository;
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
@RequestMapping("/admin/education")
public class EducationController {

    private final EducationRepository educationRepository;
    private final ProfileRepository profileRepository;

    public EducationController(EducationRepository educationRepository, ProfileRepository profileRepository) {
        this.educationRepository = educationRepository;
        this.profileRepository = profileRepository;
    }

    private Profile currentProfile(Principal principal) {
        return profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));
    }

    private Education ownedOrThrow(Long id, Principal principal) {
        Education item = educationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getProfile().getOwner().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your education entry");
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
        return "education";
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
        model.addAttribute("items", educationRepository.findByProfileIdOrderById(currentProfile(principal).getId()));
        return "admin/education/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("education", new Education());
        return "admin/education/form";
    }

    @PostMapping
    public String create(Principal principal, @Valid @ModelAttribute("education") Education education,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "admin/education/form";
        education.setProfile(currentProfile(principal));
        educationRepository.save(education);
        redirectAttributes.addFlashAttribute("success", "Education added.");
        return "redirect:/admin/education";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("education", ownedOrThrow(id, principal));
        return "admin/education/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Principal principal, @Valid @ModelAttribute("education") Education submitted,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "admin/education/form";
        Education existing = ownedOrThrow(id, principal);
        existing.setInstitution(submitted.getInstitution());
        existing.setQualification(submitted.getQualification());
        existing.setFieldOfStudy(submitted.getFieldOfStudy());
        existing.setStartDate(submitted.getStartDate());
        existing.setEndDate(submitted.getEndDate());
        existing.setDescription(submitted.getDescription());
        educationRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Education updated.");
        return "redirect:/admin/education";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        educationRepository.delete(ownedOrThrow(id, principal));
        redirectAttributes.addFlashAttribute("success", "Education removed.");
        return "redirect:/admin/education";
    }
}
