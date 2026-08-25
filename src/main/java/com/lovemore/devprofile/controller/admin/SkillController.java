package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.Skill;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.SkillRepository;
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
@RequestMapping("/admin/skills")
public class SkillController {

    private final SkillRepository skillRepository;
    private final ProfileRepository profileRepository;

    public SkillController(SkillRepository skillRepository, ProfileRepository profileRepository) {
        this.skillRepository = skillRepository;
        this.profileRepository = profileRepository;
    }

    private Profile currentProfile(Principal principal) {
        return profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));
    }

    private Skill ownedOrThrow(Long id, Principal principal) {
        Skill item = skillRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getProfile().getOwner().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your skill");
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
        return "skills";
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
        model.addAttribute("items", skillRepository.findByProfileIdOrderById(currentProfile(principal).getId()));
        return "admin/skills/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("skill", new Skill());
        model.addAttribute("levels", Skill.Level.values());
        return "admin/skills/form";
    }

    @PostMapping
    public String create(Principal principal, @Valid @ModelAttribute("skill") Skill skill,
                          BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { model.addAttribute("levels", Skill.Level.values()); return "admin/skills/form"; }
        skill.setProfile(currentProfile(principal));
        skillRepository.save(skill);
        redirectAttributes.addFlashAttribute("success", "Skill added.");
        return "redirect:/admin/skills";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("skill", ownedOrThrow(id, principal));
        model.addAttribute("levels", Skill.Level.values());
        return "admin/skills/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Principal principal, @Valid @ModelAttribute("skill") Skill submitted,
                          BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) { model.addAttribute("levels", Skill.Level.values()); return "admin/skills/form"; }
        Skill existing = ownedOrThrow(id, principal);
        existing.setName(submitted.getName());
        existing.setCategory(submitted.getCategory());
        existing.setLevel(submitted.getLevel());
        skillRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Skill updated.");
        return "redirect:/admin/skills";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        skillRepository.delete(ownedOrThrow(id, principal));
        redirectAttributes.addFlashAttribute("success", "Skill removed.");
        return "redirect:/admin/skills";
    }
}
