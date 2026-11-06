package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Récupérer tous les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Sauvegarder un utilisateur
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
