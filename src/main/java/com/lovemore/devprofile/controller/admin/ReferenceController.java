package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.Reference;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.ReferenceRepository;
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
@RequestMapping("/admin/references")
public class ReferenceController {

    private final ReferenceRepository referenceRepository;
    private final ProfileRepository profileRepository;

    public ReferenceController(ReferenceRepository referenceRepository, ProfileRepository profileRepository) {
        this.referenceRepository = referenceRepository;
        this.profileRepository = profileRepository;
    }

    private Profile currentProfile(Principal principal) {
        return profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));
    }

    private Reference ownedOrThrow(Long id, Principal principal) {
        Reference item = referenceRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getProfile().getOwner().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your reference");
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
        return "references";
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
        model.addAttribute("items", referenceRepository.findByProfileIdOrderById(currentProfile(principal).getId()));
        return "admin/references/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("reference", new Reference());
        return "admin/references/form";
    }

    @PostMapping
    public String create(Principal principal, @Valid @ModelAttribute("reference") Reference reference,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { return "admin/references/form"; }
        reference.setProfile(currentProfile(principal));
        referenceRepository.save(reference);
        redirectAttributes.addFlashAttribute("success", "Reference added.");
        return "redirect:/admin/references";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("reference", ownedOrThrow(id, principal));
        return "admin/references/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Principal principal, @Valid @ModelAttribute("reference") Reference submitted,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { return "admin/references/form"; }
        Reference existing = ownedOrThrow(id, principal);
        existing.setName(submitted.getName());
        existing.setPosition(submitted.getPosition());
        existing.setOrganisation(submitted.getOrganisation());
        existing.setEmail(submitted.getEmail());
        existing.setPhone(submitted.getPhone());
        existing.setRelationship(submitted.getRelationship());
        referenceRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Reference updated.");
        return "redirect:/admin/references";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        referenceRepository.delete(ownedOrThrow(id, principal));
        redirectAttributes.addFlashAttribute("success", "Reference removed.");
        return "redirect:/admin/references";
    }
}
