package com.fatfish.api.repository;

import com.fatfish.api.model.Installation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationRepository extends JpaRepository<Installation, UUID> {
}
