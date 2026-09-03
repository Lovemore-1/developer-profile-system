package com.lovemore.devprofile.config;

import com.lovemore.devprofile.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The whole "authentication required, unauthorised visitors can't touch
 * management functionality" requirement lives here:
 *
 *  - "/", "/u/**" (public profiles), "/register", "/login", "/verify-otp**"
 *    are open to anyone.
 *  - "/admin/**" requires a logged-in account (any authenticated user - not
 *    a specific role - because every registered user should be able to
 *    manage their OWN profile, not just a site admin). "/admin/users" is
 *    further restricted to ROLE_ADMIN.
 *  - Registration creates real accounts backed by our own User table,
 *    passwords hashed with BCrypt, never stored or compared as plain text.
 *    A freshly registered account starts emailVerified=false and can't log
 *    in (userDetailsService below marks it .disabled()) until the OTP sent
 *    to their email is entered on /verify-otp.
 *  - Google sign-in is a second, parallel way to authenticate. It's wired
 *    through GoogleOidcUserService, which creates a normal User+Profile the
 *    first time someone uses it (already marked emailVerified=true, since
 *    Google has already proven that email), so everything downstream
 *    (ownership checks, /admin, /u/{username}) treats a Google login
 *    exactly the same as a normal email/password one.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            com.lovemore.devprofile.entity.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("No such user: " + username));

            // .disabled(true) for an unverified account is what makes Spring
            // Security throw DisabledException instead of a normal bad-
            // credentials failure - LoginFailureHandler catches exactly that
            // exception and routes to /verify-otp instead of /login?error.
            UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole())
                    .disabled(!user.isEmailVerified());

            return builder.build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GoogleOidcUserService googleOidcUserService,
                                            LoginFailureHandler loginFailureHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/register", "/login", "/verify-otp", "/verify-otp/**", "/css/**", "/error", "/u/**").permitAll()
                // More specific matcher first: /admin/users needs ADMIN,
                // everything else under /admin/** just needs to be logged in.
                // Spring Security checks these in order and stops at the
                // first match, so ordering here is load-bearing.
                .requestMatchers("/admin/users").hasRole("ADMIN")
                .requestMatchers("/admin/**").authenticated()
                // H2 console is a local-only dev tool for looking inside the
                // database file directly (see application.properties). It's
                // never reachable in production because Render runs on
                // Postgres, not H2, so this route simply won't exist there.
                .requestMatchers("/h2-console/**").permitAll()
                // /oauth2/** and /login/oauth2/** (Spring Security's default
                // Google sign-in start/callback URLs) fall through to this
                // final permitAll anyway - listed here only as documentation
                // that they're intentionally open, not an oversight.
                .anyRequest().permitAll()
            )
            // The H2 console posts a login form and renders inside a frame -
            // both of which Spring Security blocks by default (CSRF check,
            // X-Frame-Options: DENY). Without these two lines the console
            // loads as a blank white page.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/admin", true)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/admin", true)
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
