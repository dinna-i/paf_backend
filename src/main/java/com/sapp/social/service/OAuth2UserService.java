package com.sapp.social.service;


import com.sapp.social.model.User;
import com.sapp.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract provider details - only Google in this case
        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");

        // Check if user exists
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Update existing user with OAuth info if needed
            User user = existingUser.get();
            if (user.getProvider() == null || !user.getProvider().equals("google")) {
                user.setProvider("google");
                user.setProviderId(providerId);
                // Optionally update profile picture
                // user.setProfilePictureUrl(pictureUrl);
                userRepository.save(user);
            }
        } else {
            // Create new user
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUserName(generateUniqueUsername(name));
            newUser.setPassword(""); // Not used for OAuth users
            newUser.setProvider("google");
            newUser.setProviderId(providerId);
            // Optionally set profile picture
            // newUser.setProfilePictureUrl(pictureUrl);
            newUser.setEnabled(true);
            userRepository.save(newUser);
        }

        return oAuth2User;
    }

    private String generateUniqueUsername(String name) {
        // Remove spaces and make lowercase
        String baseUsername = name.replaceAll("\\s+", "").toLowerCase();

        // Check if username exists
        if (!userRepository.existsByUserName(baseUsername)) {
            return baseUsername;
        }

        // Add random number until unique
        int counter = 1;
        String username = baseUsername + counter;
        while (userRepository.existsByUserName(username)) {
            counter++;
            username = baseUsername + counter;
        }

        return username;
    }
}