package com.lovemore.devprofile.controller.admin;

import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The actual admin functionality: list every account, promote/demote
 * roles, and delete accounts. Every method here is reachable only by
 * ROLE_ADMIN - enforced in SecurityConfig on /admin/users, not just by
 * this link being hidden in the sidebar for everyone else.
 *
 * Two safety rules that matter more than they might look like they do:
 * an admin can't delete their own account, and can't demote themselves.
 * Without those, it would be possible to end up with zero ADMIN accounts
 * and nobody able to reach this page ever again.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public AdminUserController(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @ModelAttribute("activePage")
    public String activePage() {
        return "users";
    }

    @ModelAttribute("profileUsername")
    public String profileUsername(Authentication authentication) {
        return authentication.getName();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users/list";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role,
                              Authentication authentication, RedirectAttributes redirectAttributes) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (target.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "You can't change your own role.");
            return "redirect:/admin/users";
        }

        target.setRole(role);
        userRepository.save(target);
        redirectAttributes.addFlashAttribute("success", "Updated " + target.getUsername() + "'s role to " + role + ".");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (target.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "You can't delete your own account while logged in as it.");
            return "redirect:/admin/users";
        }

        // Profile must go first: it holds the foreign key back to User, and
        // deleting Profile cascades (orphanRemoval) to every Project,
        // Education, Experience, Skill, Certification, Reference and Link
        // row that belongs to it. Deleting User first would fail the
        // foreign key constraint or, worse, leave orphaned data behind.
        profileRepository.findByOwnerUsername(target.getUsername()).ifPresent(profileRepository::delete);
        userRepository.delete(target);

        redirectAttributes.addFlashAttribute("success", "Deleted account: " + target.getUsername());
        return "redirect:/admin/users";
    }
}
