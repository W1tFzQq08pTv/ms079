package server.maps;

import com.github.mrzhqiang.maplestory.wz.WzData;
import org.junit.BeforeClass;
import org.junit.Test;
import server.MapleItemInformationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapleMapFactoryTest {

    @BeforeClass
    public static void loadWzData() {
        WzData.load();
    }

    @Test
    public void findsMapsInsideNestedWzDirectories() {
        MapleMapFactory factory = new MapleMapFactory();

        assertTrue(factory.findMapFile(101000000).isPresent());
        assertTrue(factory.findMapFile(100000000).isPresent());
    }

    @Test
    public void suppliesDefaultLightningResistanceForMagicWeapons() {
        assertEquals(Integer.valueOf(100), MapleItemInformationProvider.getInstance()
                .getEquipStats(1382003)
                .get("incRMAL"));
    }
}
