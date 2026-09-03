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

    private static final String ADMIN_USERNAME = "Munetsiadmin@gmail.com";
    private static final String ADMIN_PASSWORD = "password";

    // Every username this account has previously been seeded under. Kept as
    // a running list (not just the most recent one) so switching the admin
    // username again later stays a one-line change here instead of leaving
    // orphaned accounts behind each time.
    private static final String[] LEGACY_USERNAMES = { "admin", "Munetsadmin" };

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(UserRepository userRepository, ProfileRepository profileRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        removeLeftoverLegacyAdminAccounts();

        if (userRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
            return; // already seeded on a previous run
        }

        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole("ADMIN");
        admin.setEmailVerified(true); // seeded account, never goes through registration/OTP
        userRepository.save(admin);

        // Every User needs a Profile (Profile.owner is non-optional) even
        // for this utility account, or AdminProfileController's dashboard
        // would throw IllegalStateException the first time this account logs in.
        Profile profile = new Profile();
        profile.setOwner(admin);
        profile.setFullName("Site Administrator");
        profile.setHeadline("Administrator account");
        profile.setContactEmail(ADMIN_USERNAME);
        profileRepository.save(profile);

        System.out.println("Seeded admin account -> username: " + ADMIN_USERNAME + " / password: " + ADMIN_PASSWORD);
    }

    /**
     * One-time cleanup: every previous username this seeder has ever used
     * (see LEGACY_USERNAMES) gets deleted if it still exists, so there's
     * only ever the current admin login floating around, not several old
     * ones left behind from earlier in development.
     */
    private void removeLeftoverLegacyAdminAccounts() {
        for (String legacyUsername : LEGACY_USERNAMES) {
            userRepository.findByUsername(legacyUsername).ifPresent(oldAdmin -> {
                profileRepository.findByOwnerUsername(legacyUsername).ifPresent(profileRepository::delete);
                userRepository.delete(oldAdmin);
                System.out.println("Removed leftover old admin account: " + legacyUsername);
            });
        }
    }
}
