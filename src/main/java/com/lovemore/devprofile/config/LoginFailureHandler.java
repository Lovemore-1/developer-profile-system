package com.lovemore.devprofile.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Plugged into SecurityConfig's formLogin() in place of a plain failureUrl.
 * A wrong password and an unverified account both fail authentication, but
 * they need different responses: a wrong password should show the normal
 * "Incorrect email or password" message on /login, while an unverified
 * account (userDetailsService marks these .disabled(true) - see
 * SecurityConfig) should be sent to the OTP entry page instead of being
 * told their password is wrong, which it isn't.
 *
 * Spring Security throws DisabledException specifically for the disabled
 * case, so that's the one case this class handles differently; everything
 * else falls through to the same /login?error page as before.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        if (exception instanceof DisabledException) {
            String email = request.getParameter("username");
            String encoded = URLEncoder.encode(email != null ? email : "", StandardCharsets.UTF_8);
            response.sendRedirect("/verify-otp?email=" + encoded + "&unverified=true");
            return;
        }
        response.sendRedirect("/login?error");
    }
}
