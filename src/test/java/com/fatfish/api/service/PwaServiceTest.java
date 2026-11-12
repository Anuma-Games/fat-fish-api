package com.fatfish.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fatfish.api.dto.pwa.FeedbackRequest;
import com.fatfish.api.model.Feedback;
import com.fatfish.api.repository.FeedbackRepository;
import com.fatfish.api.service.PwaService.FeedbackResult;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the feedback operations of {@link PwaService}, with the repository simulated by Mockito. */
@ExtendWith(MockitoExtension.class)
class PwaServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private PwaService pwaService;

    private UUID key;
    private Feedback feedback;

    @BeforeEach
    void setUp() {
        key = UUID.randomUUID();
        feedback = new Feedback(key, "SUGERENCIA", "PWA", "Agregar el ranking semanal");
    }

    @Test
    @DisplayName("findAll: lista toda la retroalimentación guardada")
    void testFindAllFeedback() {
        // Given
        Feedback feedback2 = new Feedback(UUID.randomUUID(), "ERROR", "JUEGO", "El comodín no suma puntos");
        given(feedbackRepository.findAll()).willReturn(Arrays.asList(feedback, feedback2));

        // When
        List<Feedback> feedbackList = pwaService.findAllFeedback();

        // Then
        assertThat(feedbackList).isNotNull();
        assertThat(feedbackList.size()).isEqualTo(2);
        assertThat(feedbackList.get(0).getCategory()).isEqualTo("SUGERENCIA");
        assertThat(feedbackList.get(1).getComponent()).isEqualTo("JUEGO");
        verify(feedbackRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("create: guarda una retroalimentación nueva con sus datos")
    void testCreateFeedback() {
        // Given
        FeedbackRequest request = new FeedbackRequest("SUGERENCIA", "PWA", "  Agregar el ranking semanal  ");
        given(feedbackRepository.findByIdempotencyKey(key)).willReturn(Optional.empty());
        given(feedbackRepository.save(any(Feedback.class))).willReturn(feedback);

        // When
        FeedbackResult result = pwaService.createFeedback(key, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.created()).isTrue();
        assertThat(result.feedback().creadaEn()).isNotNull();

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository, times(1)).save(captor.capture());
        Feedback saved = captor.getValue();
        assertThat(saved.getIdempotencyKey()).isEqualTo(key);
        assertThat(saved.getCategory()).isEqualTo("SUGERENCIA");
        assertThat(saved.getComponent()).isEqualTo("PWA");
        assertThat(saved.getMessage()).isEqualTo("Agregar el ranking semanal");
    }

    @Test
    @DisplayName("create: un reintento con la misma clave no guarda otra vez")
    void testCreateFeedbackRetry() {
        // Given
        FeedbackRequest request = new FeedbackRequest("SUGERENCIA", "PWA", "Agregar el ranking semanal");
        given(feedbackRepository.findByIdempotencyKey(key)).willReturn(Optional.of(feedback));

        // When
        FeedbackResult result = pwaService.createFeedback(key, request);

        // Then
        assertThat(result.created()).isFalse();
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }
}
