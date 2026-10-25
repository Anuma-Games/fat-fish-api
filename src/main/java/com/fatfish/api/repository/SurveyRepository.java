package com.fatfish.api.repository;

import com.fatfish.api.model.Survey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    Optional<Survey> findByIdempotencyKey(UUID idempotencyKey);
}
