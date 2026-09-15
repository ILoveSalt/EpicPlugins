package hgds.epicgrief;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationSettingsTest {
    @Test
    void loadsTreasureWheelAdvancedSettings() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                options:
                  animator: wheel
                  advanced:
                    wheelCapacity: 10
                    wheelRadius: 1.9
                    recurringPrizeLimit: 2
                    wheelTrails: 6
                    ascensionMethod: OUT
                    ascensionTime: 25
                    rotationMethod: IN_OUT
                    rotationTime: 180
                    peakRotationSpeed: 40
                    descensionMethod: IN
                    descensionTime: 60
                    descentToThe: MID
                    removalInterrupt: 4
                    alwaysAdjustTheAngle: true
                    clickingSound: UI_BUTTON_CLICK
                    trailParticle: REDSTONE#
                """);

        AnimationSettings settings = AnimationSettings.fromConfig(config);

        assertTrue(settings.isEnabled());
        assertEquals(10, settings.getWheelCapacity());
        assertEquals(1.9D, settings.getWheelRadius());
        assertEquals(2, settings.getRecurringPrizeLimit());
        assertEquals(6, settings.getWheelTrails());
        assertEquals(AnimationSettings.MotionMethod.OUT, settings.getAscensionMethod());
        assertEquals(AnimationSettings.MotionMethod.IN_OUT, settings.getRotationMethod());
        assertEquals(AnimationSettings.MotionMethod.IN, settings.getDescensionMethod());
        assertEquals(40.0D, settings.getPeakRotationSpeed());
        assertEquals("MID", settings.getDescentToThe());
        assertEquals(4, settings.getRemovalInterrupt());
        assertTrue(settings.isAlwaysAdjustTheAngle());
        assertEquals(265, settings.getDurationTicks());
    }

    @Test
    void preservesOriginalSmoothMotionProfiles() {
        AnimationSettings.MotionMethod inOut = AnimationSettings.MotionMethod.IN_OUT;

        assertEquals(0.0D, inOut.movement(0.0D), 0.000001D);
        assertEquals(0.5D, inOut.movement(0.5D), 0.000001D);
        assertEquals(1.0D, inOut.movement(1.0D), 0.000001D);
        assertEquals(0.0D, inOut.speed(0.0D), 0.000001D);
        assertEquals(1.0D, inOut.speed(0.5D), 0.000001D);
        assertEquals(0.0D, inOut.speed(1.0D), 0.000001D);
        assertTrue(AnimationSettings.MotionMethod.OUT.movement(0.5D) > 0.5D);
        assertTrue(AnimationSettings.MotionMethod.IN.movement(0.5D) < 0.5D);
    }
}
