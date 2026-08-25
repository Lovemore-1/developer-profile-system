package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.Certification;
import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.repository.CertificationRepository;
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
@RequestMapping("/admin/certifications")
public class CertificationController {

    private final CertificationRepository certificationRepository;
    private final ProfileRepository profileRepository;

    public CertificationController(CertificationRepository certificationRepository, ProfileRepository profileRepository) {
        this.certificationRepository = certificationRepository;
        this.profileRepository = profileRepository;
    }

    private Profile currentProfile(Principal principal) {
        return profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));
    }

    private Certification ownedOrThrow(Long id, Principal principal) {
        Certification item = certificationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getProfile().getOwner().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your certification");
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
        return "certifications";
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
        model.addAttribute("items", certificationRepository.findByProfileIdOrderById(currentProfile(principal).getId()));
        return "admin/certifications/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("certification", new Certification());
        return "admin/certifications/form";
    }

    @PostMapping
    public String create(Principal principal, @Valid @ModelAttribute("certification") Certification certification,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { return "admin/certifications/form"; }
        certification.setProfile(currentProfile(principal));
        certificationRepository.save(certification);
        redirectAttributes.addFlashAttribute("success", "Certification added.");
        return "redirect:/admin/certifications";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("certification", ownedOrThrow(id, principal));
        return "admin/certifications/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Principal principal, @Valid @ModelAttribute("certification") Certification submitted,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { return "admin/certifications/form"; }
        Certification existing = ownedOrThrow(id, principal);
        existing.setName(submitted.getName());
        existing.setIssuer(submitted.getIssuer());
        existing.setDateEarned(submitted.getDateEarned());
        existing.setVerificationUrl(submitted.getVerificationUrl());
        certificationRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Certification updated.");
        return "redirect:/admin/certifications";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        certificationRepository.delete(ownedOrThrow(id, principal));
        redirectAttributes.addFlashAttribute("success", "Certification removed.");
        return "redirect:/admin/certifications";
    }
}
