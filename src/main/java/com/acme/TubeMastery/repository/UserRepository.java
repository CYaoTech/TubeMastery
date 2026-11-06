package com.acme.TubeMastery.repository;

import com.acme.TubeMastery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring génère automatiquement : save(), findById(), findAll(), deleteById()...
    
    // Optionnel : Vous pouvez ajouter une méthode personnalisée si besoin, Spring comprendra le SQL tout seul :
    // Optional<User> findByEmail(String email);
}
