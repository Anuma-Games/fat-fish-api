package com.fatfish.api.dto.pwa;

import java.time.Instant;

public record SurveyResponse(String id, boolean created, Instant enviadaEn) {
}
