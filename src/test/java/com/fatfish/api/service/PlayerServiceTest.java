package com.fatfish.api.service;

import static com.fatfish.api.RunRecordBuilder.aRun;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fatfish.api.dto.PageResponse;
import com.fatfish.api.dto.PlayerStatsResponse;
import com.fatfish.api.dto.RunRecord;
import com.fatfish.api.exception.PlayerNotFoundException;
import com.fatfish.api.repository.PlayerRepository;
import com.fatfish.api.repository.PlayerStatsView;
import com.fatfish.api.repository.RunRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private RunRepository runRepository;

    private PlayerService service;

    @BeforeEach
    void setUp() {
        service = new PlayerService(playerRepository, runRepository);
    }

    @Test
    void statsMapAggregatesFromRepository() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        when(runRepository.aggregateByPlayer(PLAYER_ID)).thenReturn(stats(3L, 3, 210L, 5000L, 4200L, 2900L));

        PlayerStatsResponse response = service.getStats(PLAYER_ID);

        assertThat(response).isEqualTo(new PlayerStatsResponse(PLAYER_ID, 3, 3, 210, 5000, 4200, 2900));
    }

    @Test
    void statsAreZeroWhenPlayerHasNoRuns() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        when(runRepository.aggregateByPlayer(PLAYER_ID)).thenReturn(stats(0L, null, null, null, null, null));

        PlayerStatsResponse response = service.getStats(PLAYER_ID);

        assertThat(response).isEqualTo(new PlayerStatsResponse(PLAYER_ID, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void statsFailForUnknownPlayer() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getStats(PLAYER_ID))
                .isInstanceOf(PlayerNotFoundException.class)
                .hasMessage("No existe el jugador " + PLAYER_ID);
    }

    @Test
    void historyIsPagedFromMostRecent() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        RunRecord record = aRun().playerId(PLAYER_ID).build();
        when(runRepository.findByPlayerId(eq(PLAYER_ID), any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(RunMapper.toEntity(record, PLAYER_ID)), invocation.getArgument(1), 21));

        PageResponse<RunRecord> page = service.getRuns(PLAYER_ID, 1, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(runRepository).findByPlayerId(eq(PLAYER_ID), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(1, 10,
                Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("runId"))));
        assertThat(page.content()).containsExactly(record);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void historyFailsForUnknownPlayer() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getRuns(PLAYER_ID, 0, 20)).isInstanceOf(PlayerNotFoundException.class);
    }

    private static PlayerStatsView stats(Long runs, Integer maxLevel, Long maxScore, Long bet, Long won, Long lost) {
        return new PlayerStatsView() {
            public Long getRunsPlayed() { return runs; }
            public Integer getMaxLevelReached() { return maxLevel; }
            public Long getMaxSpinScore() { return maxScore; }
            public Long getTotalBet() { return bet; }
            public Long getTotalWon() { return won; }
            public Long getTotalLost() { return lost; }
        };
    }
}
