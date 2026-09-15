package hgds.epicgrief;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

final class AnimationSettings {
    private final String animator;
    private final int wheelCapacity;
    private final double wheelRadius;
    private final int recurringPrizeLimit;
    private final int wheelTrails;
    private final MotionMethod ascensionMethod;
    private final int ascensionTime;
    private final MotionMethod rotationMethod;
    private final int rotationTime;
    private final double peakRotationSpeed;
    private final MotionMethod descensionMethod;
    private final int descensionTime;
    private final String descentToThe;
    private final int removalInterrupt;
    private final boolean alwaysAdjustTheAngle;
    private final String clickingSound;
    private final String trailParticle;

    private AnimationSettings(
            String animator,
            int wheelCapacity,
            double wheelRadius,
            int recurringPrizeLimit,
            int wheelTrails,
            MotionMethod ascensionMethod,
            int ascensionTime,
            MotionMethod rotationMethod,
            int rotationTime,
            double peakRotationSpeed,
            MotionMethod descensionMethod,
            int descensionTime,
            String descentToThe,
            int removalInterrupt,
            boolean alwaysAdjustTheAngle,
            String clickingSound,
            String trailParticle
    ) {
        this.animator = animator == null ? "none" : animator;
        this.wheelCapacity = Math.max(1, Math.min(32, wheelCapacity));
        this.wheelRadius = Math.max(0.25D, Math.min(8.0D, wheelRadius));
        this.recurringPrizeLimit = Math.max(1, Math.min(this.wheelCapacity, recurringPrizeLimit));
        this.wheelTrails = Math.max(0, Math.min(32, wheelTrails));
        this.ascensionMethod = ascensionMethod;
        this.ascensionTime = Math.max(0, ascensionTime);
        this.rotationMethod = rotationMethod;
        this.rotationTime = Math.max(0, rotationTime);
        this.peakRotationSpeed = Math.max(0.0D, Math.min(180.0D, peakRotationSpeed));
        this.descensionMethod = descensionMethod;
        this.descensionTime = Math.max(0, descensionTime);
        this.descentToThe = descentToThe == null ? "MID" : descentToThe.toUpperCase(Locale.ROOT);
        this.removalInterrupt = Math.max(1, removalInterrupt);
        this.alwaysAdjustTheAngle = alwaysAdjustTheAngle;
        this.clickingSound = clickingSound == null ? "" : clickingSound;
        this.trailParticle = trailParticle == null ? "" : trailParticle;
    }

    static AnimationSettings fromConfig(YamlConfiguration config) {
        ConfigurationSection advanced = config.getConfigurationSection("options.advanced");
        return new AnimationSettings(
                config.getString("options.animator", "none"),
                advanced == null ? 9 : advanced.getInt("wheelCapacity", 9),
                advanced == null ? 1.9D : advanced.getDouble("wheelRadius", 1.9D),
                advanced == null ? 1 : advanced.getInt("recurringPrizeLimit", 1),
                advanced == null ? 4 : advanced.getInt("wheelTrails", 4),
                MotionMethod.parse(advanced == null ? "OUT" : advanced.getString("ascensionMethod", "OUT")),
                advanced == null ? 0 : advanced.getInt("ascensionTime", 0),
                MotionMethod.parse(advanced == null ? "IN_OUT" : advanced.getString("rotationMethod", "IN_OUT")),
                advanced == null ? 60 : advanced.getInt("rotationTime", 60),
                advanced == null ? 40.0D : advanced.getDouble("peakRotationSpeed", 40.0D),
                MotionMethod.parse(advanced == null ? "OUT" : advanced.getString("descensionMethod", "OUT")),
                advanced == null ? 0 : advanced.getInt("descensionTime", 0),
                advanced == null ? "MID" : advanced.getString("descentToThe", "MID"),
                advanced == null ? 4 : advanced.getInt("removalInterrupt", 4),
                advanced == null || advanced.getBoolean("alwaysAdjustTheAngle", true),
                advanced == null ? "UI_BUTTON_CLICK" : advanced.getString("clickingSound", "UI_BUTTON_CLICK"),
                advanced == null ? "" : advanced.getString("trailParticle", "")
        );
    }

    boolean isEnabled() {
        return !"none".equalsIgnoreCase(animator) && getDurationTicks() > 0;
    }

    int getDurationTicks() {
        int duration = ascensionTime + rotationTime + descensionTime;
        return duration <= 0 ? rotationTime : duration;
    }

    String getAnimator() {
        return animator;
    }

    int getWheelCapacity() {
        return wheelCapacity;
    }

    double getWheelRadius() {
        return wheelRadius;
    }

    int getRecurringPrizeLimit() {
        return recurringPrizeLimit;
    }

    int getWheelTrails() {
        return wheelTrails;
    }

    MotionMethod getAscensionMethod() {
        return ascensionMethod;
    }

    int getAscensionTime() {
        return ascensionTime;
    }

    MotionMethod getRotationMethod() {
        return rotationMethod;
    }

    int getRotationTime() {
        return rotationTime;
    }

    double getPeakRotationSpeed() {
        return peakRotationSpeed;
    }

    MotionMethod getDescensionMethod() {
        return descensionMethod;
    }

    int getDescensionTime() {
        return descensionTime;
    }

    String getDescentToThe() {
        return descentToThe;
    }

    int getRemovalInterrupt() {
        return removalInterrupt;
    }

    boolean isAlwaysAdjustTheAngle() {
        return alwaysAdjustTheAngle;
    }

    String getClickingSound() {
        return clickingSound;
    }

    String getTrailParticle() {
        return trailParticle;
    }

    enum MotionMethod {
        IN_OUT {
            @Override
            double movement(double progress) {
                double value = clamp(progress);
                return value - Math.sin(Math.PI * 2.0D * value) / (Math.PI * 2.0D);
            }

            @Override
            double speed(double progress) {
                double value = clamp(progress);
                return (1.0D - Math.cos(Math.PI * 2.0D * value)) / 2.0D;
            }
        },
        OUT_IN {
            @Override
            double movement(double progress) {
                double value = clamp(progress);
                return value + Math.sin(Math.PI * 2.0D * value) / (Math.PI * 2.0D);
            }

            @Override
            double speed(double progress) {
                double value = clamp(progress);
                return (1.0D + Math.cos(Math.PI * 2.0D * value)) / 2.0D;
            }
        },
        IN {
            @Override
            double movement(double progress) {
                double value = clamp(progress);
                return value - Math.sin(Math.PI * value) / Math.PI;
            }

            @Override
            double speed(double progress) {
                double value = clamp(progress);
                return (1.0D - Math.cos(Math.PI * value)) / 2.0D;
            }
        },
        OUT {
            @Override
            double movement(double progress) {
                double value = clamp(progress);
                return value + Math.sin(Math.PI * value) / Math.PI;
            }

            @Override
            double speed(double progress) {
                double value = clamp(progress);
                return (1.0D + Math.cos(Math.PI * value)) / 2.0D;
            }
        },
        LINEAR {
            @Override
            double movement(double progress) {
                return clamp(progress);
            }

            @Override
            double speed(double progress) {
                return 1.0D;
            }
        },
        SINE_WAVE {
            @Override
            double movement(double progress) {
                double value = clamp(progress);
                return (1.0D - Math.cos(Math.PI * 2.0D * value)) / 2.0D;
            }

            @Override
            double speed(double progress) {
                double value = clamp(progress);
                return Math.max(0.0D, Math.sin(Math.PI * 2.0D * value));
            }
        };

        abstract double movement(double progress);

        abstract double speed(double progress);

        static MotionMethod parse(String value) {
            if (value == null || value.isBlank()) {
                return LINEAR;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
                return LINEAR;
            }
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }
}
