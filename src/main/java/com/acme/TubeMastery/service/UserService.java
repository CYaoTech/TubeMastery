package com.acme.TubeMastery.service;

import com.acme.TubeMastery.dto.RegisterRequest;
import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                "Ce pseudo est déjà utilisé."
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                "Cette adresse e-mail est déjà utilisée."
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPassword(
            passwordEncoder.encode(request.getPassword())
        );

        user.setTubecoin(0);

        return userRepository.save(user);
    }

    public boolean checkLogin(String username, String rawPassword) {

        return userRepository.findByUsername(username)
            .map(user -> passwordEncoder.matches(
                rawPassword,
                user.getPassword()
            ))
            .orElse(false);
    }
}