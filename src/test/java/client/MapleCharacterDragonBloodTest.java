package client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapleCharacterDragonBloodTest {

    @Test
    public void appliesTickOnlyWhenHpRemainsAboveOne() {
        assertTrue(MapleCharacter.canApplyDragonBloodTick(100, 20));
        assertFalse(MapleCharacter.canApplyDragonBloodTick(21, 20));
        assertFalse(MapleCharacter.canApplyDragonBloodTick(20, 20));
    }

    @Test
    public void addsShortValuesWithoutWrapping() {
        assertEquals(105, MapleCharacter.addToShort((short) 100, 5));
        assertEquals(Short.MAX_VALUE, MapleCharacter.addToShort(Short.MAX_VALUE, 1));
        assertEquals(Short.MIN_VALUE, MapleCharacter.addToShort(Short.MIN_VALUE, -1));
        assertEquals(Short.MAX_VALUE, MapleCharacter.addToShort((short) 1, Long.MAX_VALUE));
        assertEquals(Short.MIN_VALUE, MapleCharacter.addToShort((short) 0, Long.MIN_VALUE));
    }
}
