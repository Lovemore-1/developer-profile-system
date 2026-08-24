package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.Project;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Full CRUD for the one repeatable section built out this round. Every
 * method re-derives "the current user's profile" from Principal rather than
 * trusting a profileId in the URL - and edit/update/delete additionally
 * check the project actually belongs to that profile before touching it.
 * Without that check, user A could edit ?id=7 in the URL and modify user
 * B's project just by guessing an ID that exists. That check is the actual
 * security boundary in a multi-tenant system, not just the login screen.
 */
@Controller
@RequestMapping("/admin/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProfileRepository profileRepository;

    public ProjectController(ProjectRepository projectRepository, ProfileRepository profileRepository) {
        this.projectRepository = projectRepository;
        this.profileRepository = profileRepository;
    }

    private Profile currentProfile(Principal principal) {
        return profileRepository.findByOwnerUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user has no profile"));
    }

    private Project ownedProjectOrThrow(Long id, Principal principal) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!project.getProfile().getOwner().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your project");
        }
        return project;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Profile profile = currentProfile(principal);
        model.addAttribute("items", projectRepository.findByProfileIdOrderById(profile.getId()));
        return "admin/projects/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("project", new Project());
        return "admin/projects/form";
    }

    @PostMapping
    public String create(Principal principal, @Valid @ModelAttribute("project") Project project,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/projects/form";
        }
        project.setProfile(currentProfile(principal));
        projectRepository.save(project);
        redirectAttributes.addFlashAttribute("success", "Project added.");
        return "redirect:/admin/projects";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("project", ownedProjectOrThrow(id, principal));
        return "admin/projects/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Principal principal, @Valid @ModelAttribute("project") Project submitted,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/projects/form";
        }
        Project existing = ownedProjectOrThrow(id, principal);
        existing.setName(submitted.getName());
        existing.setDescription(submitted.getDescription());
        existing.setTechStack(submitted.getTechStack());
        existing.setRepoUrl(submitted.getRepoUrl());
        existing.setLiveUrl(submitted.getLiveUrl());
        projectRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Project updated.");
        return "redirect:/admin/projects";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Project project = ownedProjectOrThrow(id, principal);
        projectRepository.delete(project);
        redirectAttributes.addFlashAttribute("success", "Project removed.");
        return "redirect:/admin/projects";
    }
}
