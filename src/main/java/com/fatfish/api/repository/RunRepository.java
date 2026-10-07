package com.fatfish.api.repository;

import com.fatfish.api.model.Run;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunRepository extends JpaRepository<Run, UUID> {

    @Query("select r.runId from Run r where r.runId in :runIds")
    List<UUID> findExistingIds(@Param("runIds") Collection<UUID> runIds);

    Page<Run> findByPlayerId(UUID playerId, Pageable pageable);

    @Query("""
            select count(r) as runsPlayed,
                   max(r.levelReached) as maxLevelReached,
                   max(r.maxSpinScore) as maxSpinScore,
                   sum(r.totalBet) as totalBet,
                   sum(r.totalWon) as totalWon,
                   sum(r.totalLost) as totalLost
            from Run r
            where r.playerId = :playerId
            """)
    PlayerStatsView aggregateByPlayer(@Param("playerId") UUID playerId);
}
