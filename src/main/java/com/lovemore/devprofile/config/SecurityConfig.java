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
 *  - "/", "/u/**" (public profiles), "/register", "/login" are open to anyone.
 *  - "/admin/**" requires a logged-in account (any authenticated user - not
 *    a specific role - because every registered user should be able to
 *    manage their OWN profile, not just a site admin).
 *  - Registration creates real accounts backed by our own User table,
 *    passwords hashed with BCrypt, never stored or compared as plain text.
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

            UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole());

            return builder.build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/register", "/login", "/css/**", "/error", "/u/**").permitAll()
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
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
