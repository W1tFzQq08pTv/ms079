package client;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapleCharacterItemMoveTest {

    @Test
    public void allowsAnotherInventoryMoveAfter250Milliseconds() {
        assertFalse(MapleCharacter.isMoveItemCooldownElapsed(1_000L, 1_249L));
        assertTrue(MapleCharacter.isMoveItemCooldownElapsed(1_000L, 1_250L));
        assertTrue(MapleCharacter.isMoveItemCooldownElapsed(1_000L, 1_500L));
    }
}
