package hgds.epicgrief.bosses;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record LocationDefinition(String worldName, double x, double y, double z, float yaw, float pitch) {

    public static LocationDefinition parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split(";");
        if (parts.length < 4) {
            return null;
        }

        try {
            String worldName = parts[0].trim();
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = parts.length >= 5 ? Float.parseFloat(parts[4].trim()) : 0.0F;
            float pitch = parts.length >= 6 ? Float.parseFloat(parts[5].trim()) : 0.0F;
            return new LocationDefinition(worldName, x, y, z, yaw, pitch);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}
