package com.acme.TubeMastery.repository;

import com.acme.TubeMastery.model.UserProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
