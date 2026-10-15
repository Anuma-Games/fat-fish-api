package com.fatfish.api.repository;

import com.fatfish.api.model.PwaPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PwaPlayerRepository extends JpaRepository<PwaPlayer, Long> {
}
