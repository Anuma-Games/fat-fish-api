package com.fatfish.api.service;

import com.fatfish.api.dto.DesperateSpinDto;
import com.fatfish.api.dto.RunRecord;
import com.fatfish.api.model.DesperateSpin;
import com.fatfish.api.model.Run;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Converts between the RunRecord contract and the Run entity. */
final class RunMapper {

    private RunMapper() {
    }

    static Run toEntity(RunRecord record, UUID playerId) {
        Run run = new Run(record.runId(), record.installationId());
        run.setPlayerId(playerId);
        run.setSchemaVersion(record.schemaVersion());
        run.setGameVersion(record.gameVersion());
        run.setStartedAt(record.startedAt());
        run.setEndedAt(record.endedAt());
        run.setResult(record.result());
        run.setLevelReached(record.levelReached());
        run.setRoundsCleared(record.roundsCleared());
        run.setBossesDefeated(record.bossesDefeated());
        run.setMaxSpinScore(record.maxSpinScore());
        run.setTotalSpins(record.totalSpins());
        run.setTotalBet(record.totalBet());
        run.setTotalWon(record.totalWon());
        run.setTotalLost(record.totalLost());
        run.setAllInCount(record.allInCount());
        run.setPawnedAssets(copyOrEmpty(record.pawnedAssets()));
        run.setLoansTaken(record.loansTaken());
        run.setLoanTotal(record.loanTotal());
        run.setMostDesperateSpin(toModel(record.mostDesperateSpin()));
        run.setItemsBought(copyOrEmpty(record.itemsBought()));
        return run;
    }

    static RunRecord toRecord(Run run) {
        return new RunRecord(
                run.getSchemaVersion(),
                run.getRunId(),
                run.getInstallationId(),
                run.getPlayerId(),
                run.getGameVersion(),
                run.getStartedAt(),
                run.getEndedAt(),
                run.getResult(),
                run.getLevelReached(),
                run.getRoundsCleared(),
                run.getBossesDefeated(),
                run.getMaxSpinScore(),
                run.getTotalSpins(),
                run.getTotalBet(),
                run.getTotalWon(),
                run.getTotalLost(),
                run.getAllInCount(),
                List.copyOf(run.getPawnedAssets()),
                run.getLoansTaken(),
                run.getLoanTotal(),
                toDto(run.getMostDesperateSpin()),
                List.copyOf(run.getItemsBought()));
    }

    private static DesperateSpin toModel(DesperateSpinDto dto) {
        return dto == null ? null
                : new DesperateSpin(dto.level(), dto.round(), dto.spin(), dto.bet(), dto.moneyBefore(), dto.allIn());
    }

    private static DesperateSpinDto toDto(DesperateSpin spin) {
        return spin == null ? null
                : new DesperateSpinDto(spin.level(), spin.round(), spin.spin(), spin.bet(), spin.moneyBefore(),
                        spin.allIn());
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
