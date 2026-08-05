package server;

import com.github.mrzhqiang.maplestory.wz.WzData;
import org.junit.BeforeClass;
import org.junit.Test;

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
}
