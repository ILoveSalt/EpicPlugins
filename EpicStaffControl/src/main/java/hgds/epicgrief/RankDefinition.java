package hgds.epicgrief;

import java.util.List;

public record RankDefinition(
        String id,
        String displayName,
        String giveCommand,
        String takeCommand,
        String salaryCommand,
        long salaryAmount,
        long salaryCooldownSeconds,
        String salaryMessage,
        String salaryCooldownMessage,
        String workOnMessage,
        String workOffMessage,
        List<String> infoMessages,
        List<String> promotionMessages,
        String promotionDeniedMessage,
        String nextRank,
        long requiredWorkSeconds,
        int requiredPunishments
) {
}
