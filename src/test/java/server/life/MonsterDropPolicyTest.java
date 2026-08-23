package server.life;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MonsterDropPolicyTest {

    @Test
    public void acceleratesOnlyRareNonQuestItems() {
        MonsterDropEntry rare = drop(1000000, 10000, (short) 0);
        MonsterDropEntry common = drop(2000000, 10001, (short) 0);
        MonsterDropEntry quest = drop(4000000, 500, (short) 1000);
        MonsterDropEntry meso = drop(0, 500, (short) 0);

        assertTrue(MonsterDropPolicy.isAcceleratedRareDrop(rare));
        assertFalse(MonsterDropPolicy.isAcceleratedRareDrop(common));
        assertFalse(MonsterDropPolicy.isAcceleratedRareDrop(quest));
        assertFalse(MonsterDropPolicy.isAcceleratedRareDrop(meso));
        assertEquals(MonsterDropPolicy.CHANCE_BOUND,
                MonsterDropPolicy.effectiveChance(rare, 4320, 1));
        assertEquals(10001, MonsterDropPolicy.effectiveChance(common, 4320, 1));
        assertEquals(500, MonsterDropPolicy.effectiveChance(quest, 4320, 1));
    }

    @Test
    public void selectsAtMostOneAcceleratedDropPerMonster() {
        MonsterDropEntry firstRare = drop(1000000, 500, (short) 0);
        MonsterDropEntry secondRare = drop(1000001, 500, (short) 0);
        MonsterDropEntry common = drop(2000000, 20000, (short) 0);

        List<MonsterDropEntry> selected = MonsterDropPolicy.selectDrops(
                Arrays.asList(firstRare, secondRare, common), 4320, 1, -1, bound -> 0);

        assertEquals(Arrays.asList(firstRare, common), selected);
    }

    @Test
    public void keepsCommonAndQuestDropsAtTheirBaseChance() {
        MonsterDropEntry rare = drop(1000000, 500, (short) 0);
        MonsterDropEntry common = drop(2000000, 10001, (short) 0);
        MonsterDropEntry quest = drop(4000000, 500, (short) 1000);

        List<MonsterDropEntry> selected = MonsterDropPolicy.selectDrops(
                Arrays.asList(rare, common, quest), 4320, 1, -1, bound -> 15000);

        assertEquals(Arrays.asList(rare), selected);
    }

    @Test
    public void saturatesVeryLargeChanceMultipliers() {
        MonsterDropEntry rare = drop(1000000, 10000, (short) 0);

        assertEquals(MonsterDropPolicy.CHANCE_BOUND,
                MonsterDropPolicy.effectiveChance(rare, Integer.MAX_VALUE, Long.MAX_VALUE));
    }

    private static MonsterDropEntry drop(int itemId, int chance, short questId) {
        return new MonsterDropEntry(itemId, chance, 1, 1, questId);
    }
}
