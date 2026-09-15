package hgds.epicgrief;

import org.bukkit.util.Vector;

final class WheelAnimationGeometry {
    private static final double EPSILON = 0.000001D;

    private WheelAnimationGeometry() {
    }

    static Frame verticalFacing(Vector center, Vector viewer, Vector fallbackLookDirection) {
        Vector normal = viewer.clone().subtract(center);
        Vector fallback = fallbackLookDirection == null
                ? new Vector(0.0D, 0.0D, 1.0D)
                : fallbackLookDirection.clone();

        if (normal.lengthSquared() < EPSILON) {
            normal = fallback.clone().multiply(-1.0D);
        }
        if (normal.lengthSquared() < EPSILON) {
            normal = new Vector(0.0D, 0.0D, 1.0D);
        }
        Vector horizontal = normal.clone().setY(0.0D);
        if (horizontal.lengthSquared() < EPSILON) {
            horizontal = fallback.multiply(-1.0D).setY(0.0D);
        }
        if (horizontal.lengthSquared() < EPSILON) {
            horizontal = new Vector(0.0D, 0.0D, 1.0D);
        }
        horizontal.normalize();

        Vector right = new Vector(-horizontal.getZ(), 0.0D, horizontal.getX()).normalize();
        return new Frame(right, new Vector(0.0D, 1.0D, 0.0D));
    }

    static Vector offset(Frame frame, double radius, double angle) {
        return frame.right().clone().multiply(Math.cos(angle) * radius)
                .add(frame.up().clone().multiply(Math.sin(angle) * radius));
    }

    record Frame(Vector right, Vector up) {
    }
}
