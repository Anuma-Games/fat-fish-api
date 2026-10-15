package com.fatfish.api.repository;

import com.fatfish.api.model.PwaMatch;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PwaMatchRepository extends JpaRepository<PwaMatch, Long> {

    Page<PwaMatch> findByPlayerId(Long playerId, Pageable pageable);

    List<PwaMatch> findByPlayerIdOrderByCreatedAtAsc(Long playerId);
}
