package com.acme.TubeMastery.repository;

import com.acme.TubeMastery.service.Card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByHandle(String handle);
}
