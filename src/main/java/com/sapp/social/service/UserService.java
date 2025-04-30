package com.sapp.social.service;

import com.sapp.social.model.User;
import com.sapp.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Retrieves all users from the database
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by their ID
     * @param userId The ID of the user to retrieve
     * @return Optional containing the user if found
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Retrieves a user by their email address
     * @param email The email address to search for
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Retrieves a user by their username
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUserName(username);
    }

    /**
     * Creates a new user
     * @param user The user to create
     * @return The created user with ID
     * @throws IllegalArgumentException if email or username already exists
     */
    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (userRepository.existsByUserName(user.getUserName())) {
            throw new IllegalArgumentException("Username already in use");
        }

        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    /**
     * Updates an existing user
     * @param userId The ID of the user to update
     * @param userDetails The updated user details
     * @return The updated user
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public User updateUser(Long userId, User userDetails) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Check if email is being changed and if it's already taken
        if (!user.getEmail().equals(userDetails.getEmail()) &&
                userRepository.existsByEmail(userDetails.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Check if username is being changed and if it's already taken
        if (!user.getUserName().equals(userDetails.getUserName()) &&
                userRepository.existsByUserName(userDetails.getUserName())) {
            throw new IllegalArgumentException("Username already in use");
        }

        user.setUserName(userDetails.getUserName());
        user.setEmail(userDetails.getEmail());

        // Only update password if provided
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        user.setEnabled(userDetails.isEnabled());

        return userRepository.save(user);
    }

    /**
     * Deletes a user by their ID
     * @param userId The ID of the user to delete
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    /**
     * Finds a user by OAuth provider and provider ID
     * @param provider The OAuth provider name
     * @param providerId The provider-specific user ID
     * @return Optional containing the user if found
     */
    public Optional<User> findByProviderAndProviderId(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    /**
     * Checks if an email address is already registered
     * @param email The email address to check
     * @return true if the email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Checks if a username is already taken
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUserName(username);
    }
}
