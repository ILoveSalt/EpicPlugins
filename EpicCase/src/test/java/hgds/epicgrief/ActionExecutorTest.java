package hgds.epicgrief;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionExecutorTest {
    @Test
    void parsesLegacyActionModifiers() {
        ActionExecutor.ParsedAction action = ActionExecutor.ParsedAction.parse(
                "particle --play FLAME --executions 3 --exec-delay 4 --delay 2 "
                        + "--pause 8 --offset 1.5,2,-3 --exec-middle --dnw"
        );

        assertEquals(3, action.executions());
        assertEquals(4, action.executionDelay());
        assertEquals(2, action.delay());
        assertEquals(8, action.pause());
        assertEquals(new Vector(1.5D, 2.0D, -3.0D), action.offset());
        assertTrue(action.executeFromMiddle());
        assertTrue(action.doNotWait());
    }
}
