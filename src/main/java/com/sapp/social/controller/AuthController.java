package com.sapp.social.controller;

import com.sapp.social.dto.AuthResponse;
import com.sapp.social.dto.JwtAuthenticationResponse;
import com.sapp.social.dto.LoginRequest;
import com.sapp.social.dto.SignupRequest;
import com.sapp.social.model.User;
import com.sapp.social.repository.UserRepository;
import com.sapp.social.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        // Check if username is already taken
        if (userRepository.existsByUserName(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body("Username is already taken!");
        }

        // Check if email is already in use
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Email is already in use!");
        }

        // Create new user's account
        User user = new User();
        user.setUserName(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setEnabled(true);

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    /**
     * Endpoint for handling OAuth2 redirects
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleOauth2Callback(@RequestParam String token) {
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("success", "true");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint that returns user info for OAuth2 logins
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        // This will be accessed using the JWT token after OAuth2 login
        if (authentication != null && authentication.isAuthenticated()) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", authentication.getName());
            userInfo.put("authenticated", true);

            // Check what type of principal we have and handle accordingly
            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User oauth2User) {
                // OAuth2 login
                userInfo.put("name", oauth2User.getAttribute("name"));
                userInfo.put("authType", "oauth2");
            } else if (principal instanceof UserDetails userDetails) {
                // JWT login
                userInfo.put("name", userDetails.getUsername());
                userInfo.put("authType", "jwt");
            }

            return ResponseEntity.ok(userInfo);
        }

        return ResponseEntity.badRequest().body("User not authenticated");
    }
}