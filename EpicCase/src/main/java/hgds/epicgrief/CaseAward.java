package hgds.epicgrief;

import java.util.List;

final class CaseAward {
    private final String id;
    private final double chance;
    private final boolean rare;
    private final boolean phantom;
    private final String skipPermission;
    private final String hologramText;
    private final String hologramItem;
    private final List<String> actions;

    CaseAward(
            String id,
            double chance,
            boolean rare,
            boolean phantom,
            String skipPermission,
            String hologramText,
            String hologramItem,
            List<String> actions
    ) {
        this.id = id;
        this.chance = chance;
        this.rare = rare;
        this.phantom = phantom;
        this.skipPermission = skipPermission;
        this.hologramText = hologramText;
        this.hologramItem = hologramItem;
        this.actions = List.copyOf(actions);
    }

    String getId() {
        return id;
    }

    double getChance() {
        return chance;
    }

    boolean isRare() {
        return rare;
    }

    boolean isPhantom() {
        return phantom;
    }

    String getSkipPermission() {
        return skipPermission;
    }

    String getHologramText() {
        return hologramText;
    }

    String getHologramItem() {
        return hologramItem;
    }

    List<String> getActions() {
        return actions;
    }
}
