package com.fatfish.api.service;

import static com.fatfish.api.RunRecordBuilder.aRun;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fatfish.api.dto.RunBatchResponse;
import com.fatfish.api.dto.RunRecord;
import com.fatfish.api.model.Installation;
import com.fatfish.api.model.Player;
import com.fatfish.api.model.Run;
import com.fatfish.api.repository.InstallationRepository;
import com.fatfish.api.repository.PlayerRepository;
import com.fatfish.api.repository.RunRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    private static ValidatorFactory validatorFactory;

    @Mock
    private RunRepository runRepository;

    @Mock
    private InstallationRepository installationRepository;

    @Mock
    private PlayerRepository playerRepository;

    private RunService service;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        service = new RunService(runRepository, installationRepository, playerRepository,
                new RunRecordValidator(validatorFactory.getValidator()));
    }

    @Test
    void acceptsValidRunAndRegistersAnonymousInstallation() {
        RunRecord record = aRun().build();

        RunBatchResponse response = service.ingest(List.of(record));

        assertThat(response.accepted()).containsExactly(record.runId());
        assertThat(response.duplicates()).isEmpty();
        assertThat(response.rejected()).isEmpty();

        ArgumentCaptor<Installation> installation = ArgumentCaptor.forClass(Installation.class);
        verify(installationRepository).save(installation.capture());
        assertThat(installation.getValue().getId()).isEqualTo(record.installationId());
        assertThat(installation.getValue().getPlayerId()).isNull();

        ArgumentCaptor<Run> run = ArgumentCaptor.forClass(Run.class);
        verify(runRepository).save(run.capture());
        assertThat(run.getValue().getRunId()).isEqualTo(record.runId());
        assertThat(run.getValue().getPlayerId()).isNull();
        assertThat(run.getValue().getTotalBet()).isEqualTo(2350L);
        assertThat(run.getValue().getPawnedAssets()).containsExactly("car");
        assertThat(run.getValue().getItemsBought()).containsExactly("fresh_paint", "glass_chip");
        assertThat(run.getValue().getMostDesperateSpin().allIn()).isTrue();
        verify(playerRepository, never()).save(any());
    }

    @Nested
    class RecordValidation {

        @Test
        void rejectsLevelOutOfRange() {
            assertRejectedWith(aRun().levelReached(4).build(), "levelReached debe estar entre 1 y 3");
            assertRejectedWith(aRun().levelReached(0).build(), "levelReached debe estar entre 1 y 3");
        }

        @Test
        void rejectsNegativeSpins() {
            assertRejectedWith(aRun().totalSpins(-1).build(), "totalSpins no puede ser negativo");
        }

        @Test
        void rejectsNegativeAmounts() {
            RunRecord record = aRun().totalBet(-1L).totalWon(-5L).totalLost(-10L).loanTotal(-500L).build();

            RunBatchResponse response = service.ingest(List.of(record));

            assertThat(response.rejected()).singleElement().satisfies(rejected ->
                    assertThat(rejected.reasons()).containsExactlyInAnyOrder(
                            "totalBet no puede ser negativo",
                            "totalWon no puede ser negativo",
                            "totalLost no puede ser negativo",
                            "loanTotal no puede ser negativo"));
        }

        @Test
        void rejectsEndedAtNotAfterStartedAt() {
            Instant start = Instant.parse("2026-10-05T18:20:11Z");
            assertRejectedWith(aRun().startedAt(start).endedAt(start.minusSeconds(60)).build(),
                    "endedAt debe ser posterior a startedAt");
            assertRejectedWith(aRun().startedAt(start).endedAt(start).build(),
                    "endedAt debe ser posterior a startedAt");
        }

        @Test
        void rejectsUnknownResult() {
            assertRejectedWith(aRun().result("victory").build(),
                    "result debe ser defeat, demo_victory o abandoned");
        }

        @Test
        void acceptsEveryAllowedResult() {
            List<RunRecord> records = List.of(
                    aRun().result("defeat").build(),
                    aRun().result("demo_victory").build(),
                    aRun().result("abandoned").build());

            assertThat(service.ingest(records).accepted()).hasSize(3);
        }

        @Test
        void rejectsUnsupportedSchemaVersion() {
            assertRejectedWith(aRun().schemaVersion(2).build(), "schemaVersion 2 no soportada (se espera 1)");
        }

        @Test
        void rejectsMissingRunId() {
            assertRejectedWith(aRun().runId(null).build(), "runId es obligatorio");
        }

        @Test
        void oneInvalidRecordDoesNotRejectTheWholeBatch() {
            RunRecord valid = aRun().build();
            RunRecord invalid = aRun().levelReached(9).build();

            RunBatchResponse response = service.ingest(List.of(valid, invalid));

            assertThat(response.accepted()).containsExactly(valid.runId());
            assertThat(response.rejected()).singleElement().satisfies(rejected -> {
                assertThat(rejected.index()).isEqualTo(1);
                assertThat(rejected.runId()).isEqualTo(invalid.runId());
            });
            verify(runRepository, times(1)).save(any());
        }

        private void assertRejectedWith(RunRecord record, String reason) {
            RunBatchResponse response = service.ingest(List.of(record));

            assertThat(response.accepted()).isEmpty();
            assertThat(response.rejected()).singleElement()
                    .satisfies(rejected -> assertThat(rejected.reasons()).contains(reason));
            verify(runRepository, never()).save(any());
        }
    }

    @Nested
    class Idempotency {

        @Test
        void runAlreadyStoredIsReportedAsDuplicate() {
            RunRecord record = aRun().build();
            when(runRepository.findExistingIds(anyCollection())).thenReturn(List.of(record.runId()));

            RunBatchResponse response = service.ingest(List.of(record));

            assertThat(response.accepted()).isEmpty();
            assertThat(response.duplicates()).containsExactly(record.runId());
            verify(runRepository, never()).save(any());
        }

        @Test
        void sameRunTwiceInOneBatchIsStoredOnce() {
            RunRecord record = aRun().build();

            RunBatchResponse response = service.ingest(List.of(record, record));

            assertThat(response.accepted()).containsExactly(record.runId());
            assertThat(response.duplicates()).containsExactly(record.runId());
            verify(runRepository, times(1)).save(any());
        }

        @Test
        void sendingTheSameBatchTwiceDoesNotDuplicate() {
            RunRecord first = aRun().build();
            RunRecord second = aRun().build();
            List<RunRecord> batch = List.of(first, second);
            when(runRepository.findExistingIds(anyCollection()))
                    .thenReturn(List.of())
                    .thenReturn(List.of(first.runId(), second.runId()));

            RunBatchResponse firstResponse = service.ingest(batch);
            RunBatchResponse secondResponse = service.ingest(batch);

            assertThat(firstResponse.accepted()).containsExactly(first.runId(), second.runId());
            assertThat(secondResponse.accepted()).isEmpty();
            assertThat(secondResponse.duplicates()).containsExactly(first.runId(), second.runId());
            verify(runRepository, times(2)).save(any());
        }
    }

    @Nested
    class PlayerResolution {

        @Test
        void registersUnknownPlayerSentByTheGame() {
            UUID playerId = UUID.randomUUID();
            when(playerRepository.existsById(playerId)).thenReturn(false);

            service.ingest(List.of(aRun().playerId(playerId).build()));

            ArgumentCaptor<Player> player = ArgumentCaptor.forClass(Player.class);
            verify(playerRepository).save(player.capture());
            assertThat(player.getValue().getId()).isEqualTo(playerId);
        }

        @Test
        void usesPlayerOfLinkedInstallationWhenRecordHasNone() {
            UUID playerId = UUID.randomUUID();
            Installation installation = new Installation(UUID.randomUUID());
            installation.setPlayerId(playerId);
            when(installationRepository.findById(installation.getId())).thenReturn(Optional.of(installation));
            when(playerRepository.existsById(playerId)).thenReturn(true);

            service.ingest(List.of(aRun().installationId(installation.getId()).playerId(null).build()));

            ArgumentCaptor<Run> run = ArgumentCaptor.forClass(Run.class);
            verify(runRepository).save(run.capture());
            assertThat(run.getValue().getPlayerId()).isEqualTo(playerId);
        }

        @Test
        void rejectsRunWhenInstallationBelongsToAnotherPlayer() {
            Installation installation = new Installation(UUID.randomUUID());
            installation.setPlayerId(UUID.randomUUID());
            when(installationRepository.findById(installation.getId())).thenReturn(Optional.of(installation));
            RunRecord record = aRun().installationId(installation.getId()).playerId(UUID.randomUUID()).build();

            RunBatchResponse response = service.ingest(List.of(record));

            assertThat(response.rejected()).singleElement().satisfies(rejected ->
                    assertThat(rejected.reasons()).containsExactly("La instalación está vinculada a otro jugador"));
            verify(runRepository, never()).save(any());
        }
    }
}
