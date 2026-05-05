package com.fileload.api.controller;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import com.fileload.api.security.JwtUtil;
import com.fileload.dao.repository.UserAccountRepository;
import com.fileload.model.dto.*;
import com.fileload.model.entity.UserAccount;
import com.fileload.model.entity.UserRole;
import com.fileload.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    public AuthController(UserAccountRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordResetService = passwordResetService;
    }

    // ---------------- REGISTER ----------------
    @PostMapping("/register")
    @Operation(summary = "Register user")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        logger.info("Register API called for email: {} and username: {}", request.email(), request.username());

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getTokenVersion(), user.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(user, token));
    }

    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {

        String login = request.login().trim();

        UserAccount user = userRepository.findByEmailOrUsername(login, login)
                .orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new IllegalStateException("User is blocked");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login, request.password())
            );
        } catch (DisabledException ex) {
            throw new IllegalStateException(ex.getMessage());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getTokenVersion(), user.getRole().name());
        return ResponseEntity.ok(toAuthResponse(user, token));
    }

    // ---------------- FORGOT PASSWORD ----------------
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<ResetPasswordResponseDTO> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        try {
            passwordResetService.requestPasswordReset(request.email());

            return ResponseEntity.ok(
                    new ResetPasswordResponseDTO(true, "Password reset link sent to your email")
            );

        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResetPasswordResponseDTO(true,
                            "If an account exists, a reset link has been sent")
            );
        }
    }

    // ---------------- VALIDATE TOKEN ----------------
    @GetMapping("/validate-reset-token/{token}")
    public ResponseEntity<ResetPasswordResponseDTO> validateResetToken(@PathVariable String token) {

        boolean isValid = passwordResetService.validateResetToken(token);

        if (isValid) {
            return ResponseEntity.ok(new ResetPasswordResponseDTO(true, "Valid token"));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResetPasswordResponseDTO(false, "Invalid or expired token"));
    }

    // ---------------- RESET PASSWORD ----------------
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        try {
            passwordResetService.resetPassword(
                    request.token(),
                    request.newPassword()
            );

            return ResponseEntity.ok(
                    new ResetPasswordResponseDTO(true, "Password reset successful")
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResetPasswordResponseDTO(false, e.getMessage()));
        }
    }

    // ---------------- GOOGLE LOGIN ----------------
    @GetMapping("/oauth2/google")
    public void googleOauthLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    // ---------------- PROFILE UPLOAD ----------------
    @PostMapping("/upload-profile")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> uploadProfile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            Authentication authentication
    ) throws IOException {

        UserAccount actingUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        boolean canUpdate = actingUser.getRole() == UserRole.ADMIN || actingUser.getId().equals(userId);
        if (!canUpdate) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot update another user's profile image");
        }

        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        java.nio.file.Path path = java.nio.file.Paths.get("../uploads/" + filename);
        java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.write(path, file.getBytes());

        user.setProfileImage("/uploads/" + filename);
        userRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of("profileImage", user.getProfileImage()));
    }

    // ---------------- PROFILE GET ----------------
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AuthResponseDTO> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtUtil.generateToken(user.getEmail(), user.getTokenVersion(), user.getRole().name());
        return ResponseEntity.ok(toAuthResponse(user, token));
    }

    // ---------------- HELPER ----------------
    private AuthResponseDTO toAuthResponse(UserAccount user, String token) {
        return new AuthResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                token,
                user.getProfileImage(),
                user.getAdminPermissions()
        );
    }
}
