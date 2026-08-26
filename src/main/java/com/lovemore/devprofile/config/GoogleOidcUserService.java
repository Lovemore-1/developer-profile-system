package com.lovemore.devprofile.config;

import com.lovemore.devprofile.entity.Profile;
import com.lovemore.devprofile.entity.User;
import com.lovemore.devprofile.repository.ProfileRepository;
import com.lovemore.devprofile.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Runs right after Google confirms someone's identity, before Spring
 * Security finishes logging them in. Google only knows who someone is - it
 * has no idea about our own User/Profile tables. This bridges the two: on
 * someone's very first Google sign-in, it creates a real User row
 * (role=USER) and a starter Profile for them, exactly like
 * RegistrationController does for a normal email/password signup. Every
 * sign-in after the first just finds the existing account instead of
 * creating a duplicate.
 *
 * The username stored is Google's verified email address, so a Google
 * sign-in and a normal registration with the same email resolve to the
 * exact same account isolation rules as everywhere else in the app -
 * principal.getName() is still just "the email," regardless of which door
 * someone logged in through.
 */
@Service
public class GoogleOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleOidcUserService(UserRepository userRepository, ProfileRepository profileRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email != null && userRepository.findByUsername(email).isEmpty()) {
            User user = new User();
            user.setUsername(email);
            // Google-authenticated accounts never log in with a password of
            // their own - only through the Google button. A random hash
            // still satisfies the User table's NOT NULL password column,
            // and it can never be guessed into a valid login because the
            // password login path is never used for this account.
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole("USER");
            userRepository.save(user);

            Profile profile = new Profile();
            profile.setOwner(user);
            String name = oidcUser.getFullName();
            profile.setFullName(name != null ? name : email);
            profile.setHeadline("Add your headline in /admin");
            profile.setContactEmail(email);
            profileRepository.save(profile);
        }

        return oidcUser;
    }
}
