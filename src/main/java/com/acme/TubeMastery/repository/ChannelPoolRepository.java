package com.acme.TubeMastery.repository;

import com.acme.TubeMastery.model.ChannelPoolEntry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelPoolRepository extends JpaRepository<ChannelPoolEntry, String> {

    long countByStatusNot(ChannelPoolEntry.Status status);

    long countByStatus(ChannelPoolEntry.Status status);

    /** Chaîne vivante (non DEAD) au hasard dont les abonnés sont dans [min, max[ (null compté comme 0). */
    @Query(value = "SELECT * FROM channel_pool WHERE status <> 'DEAD' "
        + "AND COALESCE(subscriber_count, 0) >= :min AND COALESCE(subscriber_count, 0) < :max "
        + "ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<ChannelPoolEntry> findRandomAliveInRange(long min, long max);

    @Query(value = "SELECT * FROM channel_pool WHERE status = :status ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<ChannelPoolEntry> findRandomByStatus(String status);

    @Query(value = "SELECT * FROM channel_pool WHERE status <> 'DEAD' ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<ChannelPoolEntry> findRandomAlive();
}
