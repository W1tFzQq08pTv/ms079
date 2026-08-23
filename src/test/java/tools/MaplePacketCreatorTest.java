package tools;

import client.MapleBuffStat;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaplePacketCreatorTest {

    @Test
    public void comboPacketLayoutFollowsTheActualBuffStat() {
        List<Pair<MapleBuffStat, Integer>> combo = Collections.singletonList(
                new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, 2));
        List<Pair<MapleBuffStat, Integer>> defense = Collections.singletonList(
                new Pair<MapleBuffStat, Integer>(MapleBuffStat.WDEF, 40));

        assertTrue(MaplePacketCreator.usesCompactBuffTail(combo, null));
        assertFalse(MaplePacketCreator.usesCompactBuffTail(defense, null));
    }

    @Test
    public void finalAttackPacketLayoutFollowsTheActualBuffStat() {
        List<Pair<MapleBuffStat, Integer>> finalAttack = Collections.singletonList(
                new Pair<MapleBuffStat, Integer>(MapleBuffStat.FINALATTACK, 1));

        assertTrue(MaplePacketCreator.usesCompactBuffTail(finalAttack, null));
    }
}
