package server.maps;

import com.github.mrzhqiang.maplestory.wz.WzData;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class MapleReactorFactoryTest {

    @BeforeClass
    public static void loadWzData() {
        WzData.load();
    }

    @Test
    public void loadsMapleIslandQuestBoxHitSequence() {
        MapleReactorStats stats = MapleReactorFactory.getReactor(2001);

        assertEquals(0, stats.getType((byte) 0));
        assertEquals(1, stats.getNextState((byte) 0));
        assertEquals(2, stats.getNextState((byte) 1));
        assertEquals(3, stats.getNextState((byte) 2));
        assertEquals(4, stats.getNextState((byte) 3));
        assertEquals(999, stats.getType((byte) 4));
        assertEquals(-1, stats.getNextState((byte) 4));
    }

    @Test
    public void preservesExplicitLinkedReactorTemplates() {
        assertSame(MapleReactorFactory.getReactor(2000),
                MapleReactorFactory.getReactor(1002001));
    }
}
