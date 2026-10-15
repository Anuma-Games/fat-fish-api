package com.fatfish.api.dto.pwa;

import java.time.Instant;

/** Server-generated id (as text, per the PWA contract) and creation date. */
public record FeedbackResponse(String id, Instant creadaEn) {
}
