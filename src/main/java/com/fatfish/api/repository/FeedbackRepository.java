package com.fatfish.api.repository;

import com.fatfish.api.model.Feedback;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByIdempotencyKey(UUID idempotencyKey);
}
