package com.fatfish.api.repository;

import com.fatfish.api.model.Player;
import com.fatfish.api.model.Run;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    
   @Query(value = """
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM jugador
            WHERE id = :playerId
        ) THEN 1
        ELSE 0
    END
    """, nativeQuery = true)
int createNewPlayer(@Param("playerId") int playerId);
}





