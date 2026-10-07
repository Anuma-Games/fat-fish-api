package com.fatfish.api.service;

import com.fatfish.api.dto.RejectedRun;
import com.fatfish.api.dto.RunBatchResponse;
import com.fatfish.api.dto.RunRecord;
import com.fatfish.api.model.Installation;
import com.fatfish.api.model.Player;
import com.fatfish.api.repository.InstallationRepository;
import com.fatfish.api.repository.PlayerRepository;
import com.fatfish.api.repository.RunRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests run batches from the game's outbox. Idempotent by runId: a run that already
 * exists (or appears twice in the same batch) is reported as a duplicate and not stored again.
 */
@Service
public class RunService {

    private final RunRepository runRepository;
    private final InstallationRepository installationRepository;
    private final PlayerRepository playerRepository;
    private final RunRecordValidator validator;

    public RunService(RunRepository runRepository, InstallationRepository installationRepository,
            PlayerRepository playerRepository, RunRecordValidator validator) {
        this.runRepository = runRepository;
        this.installationRepository = installationRepository;
        this.playerRepository = playerRepository;
        this.validator = validator;
    }

    @Transactional
    public RunBatchResponse ingest(List<RunRecord> records) {
        List<UUID> accepted = new ArrayList<>();
        List<UUID> duplicates = new ArrayList<>();
        List<RejectedRun> rejected = new ArrayList<>();

        Set<UUID> knownRunIds = findExistingRunIds(records);
        Map<UUID, Installation> installations = new HashMap<>();
        Set<UUID> knownPlayers = new HashSet<>();

        for (int i = 0; i < records.size(); i++) {
            RunRecord record = records.get(i);
            List<String> reasons = validator.validate(record);
            if (!reasons.isEmpty()) {
                rejected.add(new RejectedRun(i, record == null ? null : record.runId(), reasons));
                continue;
            }
            if (!knownRunIds.add(record.runId())) {
                duplicates.add(record.runId());
                continue;
            }

            Installation installation = findOrCreateInstallation(record.installationId(), installations);
            if (installation.getPlayerId() != null && record.playerId() != null
                    && !installation.getPlayerId().equals(record.playerId())) {
                knownRunIds.remove(record.runId());
                rejected.add(new RejectedRun(i, record.runId(),
                        List.of("La instalación está vinculada a otro jugador")));
                continue;
            }

            UUID playerId = record.playerId() != null ? record.playerId() : installation.getPlayerId();
            if (playerId != null) {
                ensurePlayerExists(playerId, knownPlayers);
            }
            runRepository.save(RunMapper.toEntity(record, playerId));
            accepted.add(record.runId());
        }

        return new RunBatchResponse(accepted, duplicates, rejected);
    }

    private Set<UUID> findExistingRunIds(List<RunRecord> records) {
        List<UUID> runIds = records.stream()
                .filter(Objects::nonNull)
                .map(RunRecord::runId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return runIds.isEmpty() ? new HashSet<>() : new HashSet<>(runRepository.findExistingIds(runIds));
    }

    private Installation findOrCreateInstallation(UUID installationId, Map<UUID, Installation> cache) {
        Installation installation = cache.get(installationId);
        if (installation == null) {
            installation = installationRepository.findById(installationId).orElse(null);
            if (installation == null) {
                // First contact from this installation: register it anonymously (player_id stays null)
                installation = new Installation(installationId);
                installationRepository.save(installation);
            }
            cache.put(installationId, installation);
        }
        return installation;
    }

    /**
     * Provisional until authentication and installation linking exist:
     * an unknown playerId sent by the game is registered on the fly.
     */
    private void ensurePlayerExists(UUID playerId, Set<UUID> knownPlayers) {
        if (knownPlayers.contains(playerId)) {
            return;
        }
        if (!playerRepository.existsById(playerId)) {
            playerRepository.save(new Player(playerId));
        }
        knownPlayers.add(playerId);
    }
}
