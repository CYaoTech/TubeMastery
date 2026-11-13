package com.acme.TubeMastery.repository;

import com.acme.TubeMastery.model.Auction;
import com.acme.TubeMastery.model.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatusOrderByEndsAtAsc(Auction.Status status);

    List<Auction> findByStatusAndEndsAtBefore(Auction.Status status, Instant instant);

    List<Auction> findBySellerAndStatus(User seller, Auction.Status status);

    /** Verrou d'écriture : sérialise les mises concurrentes et la clôture. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :id")
    Optional<Auction> findByIdForUpdate(Long id);
}
