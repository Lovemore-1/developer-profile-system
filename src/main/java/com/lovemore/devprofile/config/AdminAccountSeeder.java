package com.lovemore.devprofile.config;

import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once on every startup and makes sure exactly one ADMIN account
 * exists. Self-registration (RegistrationController) always creates
 * role="USER" accounts - there is no signup form for ADMIN, on purpose.
 * The only way to become ADMIN is to already be an admin and promote
 * someone (or be this seeded account). That's what makes the role
 * "real" rather than decorative: a normal user can never grant
 * themselves admin through the UI.
 *
 * Username/password are hardcoded here because this is a local demo
 * project with a hard deadline, not a production system. In a real
 * deployment these would come from environment variables and the
 * password would be rotated after first login.
 */
@Component
public class AdminAccountSeeder implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "Munetsadmin";
    private static final String ADMIN_PASSWORD = "password";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(UserRepository userRepository, ProfileRepository profileRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** The original hardcoded username from an earlier version of this seeder. */
    private static final String OLD_ADMIN_USERNAME = "admin";

    @Override
    public void run(String... args) {
        removeLeftoverOldAdminAccount();

        if (userRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
            return; // already seeded on a previous run
        }

        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole("ADMIN");
        userRepository.save(admin);

        // Every User needs a Profile (Profile.owner is non-optional) even
        // for this utility account, or AdminProfileController's dashboard
        // would throw IllegalStateException the first time this account logs in.
        Profile profile = new Profile();
        profile.setOwner(admin);
        profile.setFullName("Site Administrator");
        profile.setHeadline("Administrator account");
        profile.setContactEmail("admin@devprofile.local");
        profileRepository.save(profile);

        System.out.println("Seeded admin account -> username: " + ADMIN_USERNAME + " / password: " + ADMIN_PASSWORD);
    }

    /**
     * One-time cleanup: an earlier version of this class always seeded the
     * account as "admin" / "admin123". Since the username changed, delete
     * that leftover account (and its profile) so there's only ever one
     * admin login floating around, not two. Once it's gone this is a no-op
     * on every future startup.
     */
    private void removeLeftoverOldAdminAccount() {
        userRepository.findByUsername(OLD_ADMIN_USERNAME).ifPresent(oldAdmin -> {
            profileRepository.findByOwnerUsername(OLD_ADMIN_USERNAME).ifPresent(profileRepository::delete);
            userRepository.delete(oldAdmin);
            System.out.println("Removed leftover old admin account: " + OLD_ADMIN_USERNAME);
        });
    }
}
