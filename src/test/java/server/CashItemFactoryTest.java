package server;

import client.inventory.Item;
import com.github.mrzhqiang.maplestory.wz.WzData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class CashItemFactoryTest {

    @BeforeClass
    public static void loadWzData() {
        WzData.load();
    }

    @Test
    public void readsPackageMemberSerialsFromWz() {
        assertEquals(Arrays.asList(20000155, 20400079, 20500079, 20600044, 20700007, 21100015),
                CashItemFactory.getPackageSerials(9100000));
    }

    @Test
    public void preservesDuplicatePackageMemberSerials() {
        assertEquals(Collections.nCopies(10, 50400007), CashItemFactory.getPackageSerials(9101289));
    }

    @Test
    public void resolvesVisibleHistoricalPackageAlias() {
        CashItemInfo item = new CashItemInfo(9101740, 1, 3400, 70000123, 0, 2, false);

        assertSame(item, CashItemFactory.resolveHistoricalCatalogPackage(70000123, item));
        assertEquals(Arrays.asList(20000478, 20300243, 50500064),
                CashItemFactory.getPackageSerials(item.getId()));
    }

    @Test
    public void rejectsPackageAliasNotShownInHistoricalCatalog() {
        CashItemInfo item = new CashItemInfo(9101740, 1, 3400, 10001476, 0, 2, false);

        assertNull(CashItemFactory.resolveHistoricalCatalogPackage(10001476, item));
    }

    @Test
    public void preservesConfiguredThirtyDayPeriodWithoutOverflow() {
        CashItemInfo item = new CashItemInfo(1002200, 1, 460, 20000155, 30, 2, true);

        assertEquals(TimeUnit.DAYS.toMillis(30), CashShop.cashItemDurationMillis(item));
    }

    @Test
    public void usesThreeHourDurationForThreeHourExpCard() {
        CashItemInfo item = new CashItemInfo(5211047, 1, 100, 10000900, 0, 2, true);

        assertEquals(TimeUnit.HOURS.toMillis(3), CashShop.cashItemDurationMillis(item));
    }

    @Test
    public void resolvesCommodityMatchingQuantityAndDuration() {
        Item item = new Item(5041000, (short) 0, (short) 30, (byte) 2, 110);
        item.setExpiration(-1L);
        CashItemInfo timedTwoPack = new CashItemInfo(5041000, 2, 0, 30200033, 7, 2, true);
        CashItemInfo permanentThirtyPack = new CashItemInfo(5041000, 30, 5800, 10000553, 0, 2, true);

        assertSame(permanentThirtyPack,
                CashItemFactory.findMatchingCashItem(item, Arrays.asList(timedTwoPack, permanentThirtyPack)));
    }

    @Test
    public void doesNotResolveCommodityWithDifferentDuration() {
        Item item = new Item(5041000, (short) 0, (short) 2, (byte) 2, 110);
        item.setExpiration(-1L);
        CashItemInfo timedTwoPack = new CashItemInfo(5041000, 2, 0, 30200033, 7, 2, true);

        assertNull(CashItemFactory.findMatchingCashItem(item, Collections.singletonList(timedTwoPack)));
    }
}
