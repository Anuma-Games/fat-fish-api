package com.fatfish.api.repository;

import com.fatfish.api.model.PlayerStatistics;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {

    @Query(value = """
            select s.playerId as playerId, p.alias as alias, s.bestScore as bestScore, s.bestScoreAt as bestScoreAt
            from PlayerStatistics s join Player p on p.id = s.playerId
            where s.bestScoreAt is not null
            order by s.bestScore desc, s.bestScoreAt asc, s.playerId asc
            """,
            countQuery = """
            select count(s) from PlayerStatistics s where s.bestScoreAt is not null
            """)
    Page<RankingRow> findRanking(Pageable pageable);

    @Query("""
            select s.playerId as playerId, p.alias as alias, s.bestScore as bestScore, s.bestScoreAt as bestScoreAt
            from PlayerStatistics s join Player p on p.id = s.playerId
            where s.playerId = :playerId and s.bestScoreAt is not null
            """)
    Optional<RankingRow> findRankingRow(@Param("playerId") Long playerId);

    @Query("""
            select count(s) from PlayerStatistics s
            where s.bestScoreAt is not null
              and (s.bestScore > :score
                or (s.bestScore = :score and s.bestScoreAt < :achievedAt)
                or (s.bestScore = :score and s.bestScoreAt = :achievedAt and s.playerId < :playerId))
            """)
    long countAhead(@Param("score") int score, @Param("achievedAt") Instant achievedAt,
            @Param("playerId") Long playerId);

    interface RankingRow {

        Long getPlayerId();

        String getAlias();

        int getBestScore();

        Instant getBestScoreAt();
    }
}