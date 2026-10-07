package com.fatfish.api.dto;

import java.util.List;
import java.util.UUID;

/** Outcome per runId. The game treats accepted and duplicates as synced. */
public record RunBatchResponse(List<UUID> accepted, List<UUID> duplicates, List<RejectedRun> rejected) {
}
