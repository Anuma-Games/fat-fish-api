package com.fatfish.api.repository;

import com.fatfish.api.model.Match;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Page<Match> findByPlayerId(Long playerId, Pageable pageable);

    List<Match> findByPlayerIdOrderByLocalNumberAsc(Long playerId);

    Optional<Match> findByPlayerIdAndLocalNumber(Long playerId, int localNumber);
}
