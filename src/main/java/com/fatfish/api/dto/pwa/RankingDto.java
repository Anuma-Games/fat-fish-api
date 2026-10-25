package com.fatfish.api.dto.pwa;

public record RankingDto(PwaPage<RankingEntryDto> pagina, RankingEntryDto miPosicion) {
}
