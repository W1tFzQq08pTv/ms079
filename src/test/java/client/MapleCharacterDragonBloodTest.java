package client;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapleCharacterDragonBloodTest {

    @Test
    public void appliesTickOnlyWhenHpRemainsAboveOne() {
        assertTrue(MapleCharacter.canApplyDragonBloodTick(100, 20));
        assertFalse(MapleCharacter.canApplyDragonBloodTick(21, 20));
        assertFalse(MapleCharacter.canApplyDragonBloodTick(20, 20));
    }
}
