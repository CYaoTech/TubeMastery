package com.acme.TubeMastery.controller;

import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        User savedUser = userService.registerUser(user);
        return ResponseEntity.ok("Utilisateur enregistré avec succès ! ID: " + savedUser.getIdUser());
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        boolean isAuthenticated = userService.checkLogin(username, password);
        
        if (isAuthenticated) {
            return ResponseEntity.ok("Connexion réussie ! Bienvenue sur TubeMastery.");
        } else {
            return ResponseEntity.status(401).body("Échec de la connexion : Pseudo ou mot de passe incorrect.");
        }
    }
}
