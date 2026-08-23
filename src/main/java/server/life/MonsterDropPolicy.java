package server.life;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Applies the configured multiplier only to rare, non-quest drops and limits
 * accelerated drops to one item per monster.
 */
public final class MonsterDropPolicy {

    public static final int CHANCE_BOUND = 999999;
    public static final int RARE_DROP_MAX_CHANCE = 10000;

    private MonsterDropPolicy() {
    }

    public static boolean isAcceleratedRareDrop(MonsterDropEntry entry) {
        return entry.itemId > 0
                && entry.questid <= 0
                && entry.chance > 0
                && entry.chance <= RARE_DROP_MAX_CHANCE;
    }

    public static long effectiveChance(MonsterDropEntry entry, int rareRate, long dropModifier) {
        long multiplier = isAcceleratedRareDrop(entry) ? Math.max(1, rareRate) : 1;
        long scaledChance = Math.min(CHANCE_BOUND, Math.max(0L, entry.chance) * multiplier);
        long cappedModifier = Math.min(CHANCE_BOUND, Math.max(0L, dropModifier));
        return Math.min(CHANCE_BOUND, scaledChance * cappedModifier);
    }

    public static List<MonsterDropEntry> selectDrops(List<MonsterDropEntry> entries, int rareRate,
            long dropModifier, int stolenItemId, IntUnaryOperator random) {
        List<MonsterDropEntry> selected = new ArrayList<MonsterDropEntry>();
        boolean acceleratedDropSelected = false;
        for (MonsterDropEntry entry : entries) {
            if (entry.itemId == stolenItemId) {
                continue;
            }
            boolean accelerated = isAcceleratedRareDrop(entry);
            if (accelerated && acceleratedDropSelected) {
                continue;
            }
            if (random.applyAsInt(CHANCE_BOUND) < effectiveChance(entry, rareRate, dropModifier)) {
                selected.add(entry);
                acceleratedDropSelected |= accelerated;
            }
        }
        return selected;
    }
}
