package com.fatfish.api.repository;

import com.fatfish.api.model.Player;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByTokenHash(String tokenHash);

     @Query("""
            select p from Player p
            where locate(lower(:alias), lower(p.alias)) > 0
            order by p.lastActivity desc nulls last, p.id asc
            """)
    List<Player> searchByAlias(@Param("alias") String alias, Limit limit);
}
