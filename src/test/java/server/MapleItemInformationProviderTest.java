package server;

import com.github.mrzhqiang.maplestory.wz.WzData;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MapleItemInformationProviderTest {

    @BeforeClass
    public static void loadWzData() {
        WzData.load();
    }

    @Test
    public void loadsEffectsFromGroupedConsumeFiles() {
        MapleItemInformationProvider provider = MapleItemInformationProvider.getInstance();

        assertNotNull(provider.getItemEffect(2000002));
        assertNotNull(provider.getItemEffect(2000006));
        assertNotNull(provider.getItemEffect(2010004));
    }

    @Test
    public void readsNpcSellPricesForEquipment() {
        MapleItemInformationProvider provider = MapleItemInformationProvider.getInstance();

        assertEquals(20_000.0, provider.getPrice(1072101), 0.0);
        assertEquals(15_000.0, provider.getPrice(1002013), 0.0);
        assertEquals(35_000.0, provider.getPrice(1050030), 0.0);
        assertEquals(0.3, provider.getPrice(2070000), 0.0);
    }
}
