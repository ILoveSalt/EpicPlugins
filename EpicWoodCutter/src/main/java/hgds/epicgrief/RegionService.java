package hgds.epicgrief;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class RegionService {

    private final JavaPlugin plugin;
    private final Map<String, WoodRegionSettings> regions = new HashMap<>();
    private RegionContainer regionContainer;
    private boolean warningShown;

    RegionService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void loadRegions(Map<String, WoodRegionSettings> loadedRegions) {
        regions.clear();
        regions.putAll(loadedRegions);
        reloadWorldGuard();
    }

    WoodRegionSettings findRegion(Location location) {
        if (regionContainer == null || location == null || regions.isEmpty()) {
            return null;
        }

        RegionQuery query = regionContainer.createQuery();
        ApplicableRegionSet applicableRegions = query.getApplicableRegions(BukkitAdapter.adapt(location));

        WoodRegionSettings bestMatch = null;
        int bestPriority = Integer.MIN_VALUE;

        for (ProtectedRegion protectedRegion : applicableRegions) {
            WoodRegionSettings settings = regions.get(protectedRegion.getId().toLowerCase(Locale.ROOT));
            if (settings == null) {
                continue;
            }

            if (protectedRegion.getPriority() >= bestPriority) {
                bestPriority = protectedRegion.getPriority();
                bestMatch = settings;
            }
        }

        return bestMatch;
    }

    boolean isInWoodRegion(Location location) {
        return findRegion(location) != null;
    }

    private void reloadWorldGuard() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            regionContainer = null;
            if (!warningShown) {
                plugin.getLogger().warning("WorldGuard не найден. Регионы лесоруба работать не будут.");
                warningShown = true;
            }
            return;
        }

        regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }
}
