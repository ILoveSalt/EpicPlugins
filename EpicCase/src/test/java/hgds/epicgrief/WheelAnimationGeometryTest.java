package hgds.epicgrief;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WheelAnimationGeometryTest {
    private static final double EPSILON = 0.000001D;

    @Test
    void keepsWheelVerticalForViewerAtSameHeight() {
        WheelAnimationGeometry.Frame frame = WheelAnimationGeometry.verticalFacing(
                new Vector(0.0D, 2.0D, 0.0D),
                new Vector(0.0D, 2.0D, 4.0D),
                new Vector(0.0D, 0.0D, -1.0D)
        );

        assertEquals(0.0D, frame.right().getY(), EPSILON);
        assertEquals(0.0D, frame.up().getX(), EPSILON);
        assertEquals(1.0D, frame.up().getY(), EPSILON);
        assertEquals(0.0D, frame.up().getZ(), EPSILON);
    }

    @Test
    void keepsWheelFixedAboveCaseWithoutMovingTowardViewer() {
        Vector center = new Vector(3.0D, 5.0D, -2.0D);
        Vector viewer = new Vector(3.0D, 2.0D, 4.0D);
        WheelAnimationGeometry.Frame frame = WheelAnimationGeometry.verticalFacing(
                center,
                viewer,
                new Vector(0.0D, 0.0D, -1.0D)
        );

        assertEquals(1.0D, frame.right().length(), EPSILON);
        assertEquals(1.0D, frame.up().length(), EPSILON);
        assertEquals(0.0D, frame.right().getY(), EPSILON);
        assertEquals(1.0D, frame.up().getY(), EPSILON);
        assertEquals(0.0D, frame.up().getX(), EPSILON);
        assertEquals(0.0D, frame.up().getZ(), EPSILON);

        double radius = 1.9D;
        Vector first = WheelAnimationGeometry.offset(frame, radius, 0.37D);
        Vector opposite = WheelAnimationGeometry.offset(frame, radius, 0.37D + Math.PI);
        assertEquals(radius, first.length(), EPSILON);
        assertEquals(radius, opposite.length(), EPSILON);
        assertEquals(0.0D, first.clone().add(opposite).length(), EPSILON);
        assertEquals(0.0D, first.dot(viewer.clone().subtract(center).setY(0.0D)), EPSILON);
    }
}
