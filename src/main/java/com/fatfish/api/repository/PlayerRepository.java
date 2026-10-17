package com.fatfish.api.repository;

import com.fatfish.api.model.Player;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByTokenHash(String tokenHash);
}
