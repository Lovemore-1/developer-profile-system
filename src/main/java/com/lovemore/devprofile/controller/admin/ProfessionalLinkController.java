package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.ProfessionalLink;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.ProfessionalLinkRepository;
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
@RequestMapping("/admin/links")
public class ProfessionalLinkController {

    private final ProfessionalLinkRepository linkRepository;
    private final ProfileRepository profileRepository;

    public ProfessionalLinkController(ProfessionalLinkRepository linkRepository, ProfileRepository profileRepository) {
        this.linkRepository = linkRepository;
        this.profileRepository = profileRepository;
    }

    private Profile currentProfile(Principal principal) {
        return profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));
    }

    private ProfessionalLink ownedOrThrow(Long id, Principal principal) {
        ProfessionalLink item = linkRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getProfile().getOwner().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your link");
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
        return "links";
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
        model.addAttribute("items", linkRepository.findByProfileIdOrderById(currentProfile(principal).getId()));
        return "admin/links/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("link", new ProfessionalLink());
        return "admin/links/form";
    }

    @PostMapping
    public String create(Principal principal, @Valid @ModelAttribute("link") ProfessionalLink link,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { return "admin/links/form"; }
        link.setProfile(currentProfile(principal));
        linkRepository.save(link);
        redirectAttributes.addFlashAttribute("success", "Link added.");
        return "redirect:/admin/links";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("link", ownedOrThrow(id, principal));
        return "admin/links/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Principal principal, @Valid @ModelAttribute("link") ProfessionalLink submitted,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { return "admin/links/form"; }
        ProfessionalLink existing = ownedOrThrow(id, principal);
        existing.setLabel(submitted.getLabel());
        existing.setUrl(submitted.getUrl());
        linkRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Link updated.");
        return "redirect:/admin/links";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        linkRepository.delete(ownedOrThrow(id, principal));
        redirectAttributes.addFlashAttribute("success", "Link removed.");
        return "redirect:/admin/links";
    }
}
