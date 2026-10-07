package com.fatfish.api.dto;

import java.util.List;
import java.util.UUID;

/** A record that failed validation. The game marks it as rejected and does not retry it. */
public record RejectedRun(int index, UUID runId, List<String> reasons) {
}
