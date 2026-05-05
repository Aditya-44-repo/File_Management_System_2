package com.fileload.api.security;

import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import com.fileload.dao.repository.UserAccountRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    @Value("${app.frontend-base-url:https://localhost:4200}")
    private String frontendBaseUrl;

    private final JwtUtil jwtUtil;
    private final UserAccountRepository userRepository;

    public OAuth2SuccessHandler(JwtUtil jwtUtil, UserAccountRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            // Extract user info from OAuth2User (handles different Google response formats)
            String email = extractEmail(oAuth2User);
            String name = extractName(oAuth2User);

            if (email == null || email.isEmpty()) {
                sendErrorRedirect(response, "Email not available from OAuth provider");
                return;
            }

            // Check if user exists, if not create new user
            UserAccount user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        UserAccount newUser = new UserAccount();
                        newUser.setPassword(""); // OAuth2 users don't have password
                        newUser.setUsername(name != null && !name.isEmpty() ? name : email.split("@")[0]);
                        newUser.setPassword("");
                        newUser.setRole(UserRole.USER);
                        return userRepository.save(newUser);
                    });

            if (!user.isEnabled()) {
                sendErrorRedirect(response, blockedMessage(user));
                return;
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getTokenVersion(), user.getRole().name());

            // Redirect to frontend callback with token + user details
            String redirectUrl = UriComponentsBuilder.fromHttpUrl(buildFrontendUrl("/oauth/callback"))
                    .queryParam("token", token)
                    .queryParam("email", user.getEmail())
                    .queryParam("username", user.getUsername())
                    .queryParam("id", user.getId())
                    .queryParam("role", user.getRole().name())
                    .queryParam("adminPermissions", user.getAdminPermissions())
                    .build()
                    .toUriString();
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            logger.error("OAuth authentication failed", e);
            sendErrorRedirect(response, "OAuth authentication failed: " + e.getMessage());
        }
    }

    private String extractEmail(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        if (attributes.containsKey("email")) {
            return (String) attributes.get("email");
        }
        return oAuth2User.getAttribute("email");
    }

    private String extractName(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        if (attributes.containsKey("name")) {
            return (String) attributes.get("name");
        }
        if (attributes.containsKey("given_name")) {
            return (String) attributes.get("given_name");
        }
        return oAuth2User.getAttribute("name");
    }

    private void sendErrorRedirect(HttpServletResponse response, String errorMessage) throws IOException {
        String redirectUrl = UriComponentsBuilder.fromHttpUrl(buildFrontendUrl("/login"))
                .queryParam("error", errorMessage)
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private String buildFrontendUrl(String path) {
        String base = frontendBaseUrl == null ? "https://localhost:4200" : frontendBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private String blockedMessage(UserAccount user) {
        return user.getDisabledByRole() == UserRole.SUPER_ADMIN
                ? "You are blocked by superadmin"
                : "You are blocked by admin";
    }
}

