package server;

import com.github.mrzhqiang.maplestory.domain.DCashShopModifiedItem;
import com.github.mrzhqiang.maplestory.wz.WzData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CashItemFactoryTest {

    @BeforeClass
    public static void loadWzData() {
        WzData.load();
    }

    @Test
    public void readsNestedPackageSerialNumbers() {
        CashItemFactory factory = new CashItemFactory();

        assertEquals(Arrays.asList(20000155, 20400079, 20500079,
                        20600044, 20700007, 21100015),
                factory.getPackageSerialNumbers(9100000));
        assertEquals(Arrays.asList(50400007, 50400007, 50400007, 50400007, 50400007,
                        50400007, 50400007, 50400007, 50400007, 50400007),
                factory.getPackageSerialNumbers(9101289));
    }

    @Test
    public void loadsOnlyConfiguredCashShopModifications() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem item = modification(10000001, -1, true);
        DCashShopModifiedItem hiddenItem = modification(10000002, -1, false);

        factory.loadModifications(Arrays.asList(item, hiddenItem));

        assertNotNull(factory.getModInfo(10000001));
        assertEquals(-1, factory.getModInfo(10000001).mark);
        assertNull(factory.getModInfo(10000002));
    }

    @Test
    public void excludesOrdinaryEquipmentFromCashShopModifications() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem modification = modification(80000001, -1, true);
        CashItemInfo ordinaryEquipment = new CashItemInfo(
                1302016, 1, 0, 80000001, 0, 2, false);

        factory.loadModifications(Arrays.asList(modification),
                Arrays.asList(ordinaryEquipment));

        assertNull(factory.getModInfo(80000001));
    }

    @Test
    public void excludesZeroPriceCashShopModifications() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem modification = modification(10000001, -1, true);
        CashItemInfo commodity = new CashItemInfo(
                1001000, 1, 0, 10000001, 0, 2, false);

        factory.loadModifications(Arrays.asList(modification), Arrays.asList(commodity));

        assertNull(factory.getModInfo(10000001));
    }

    @Test
    public void excludesNonCashConsumablesFromCashShopModifications() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem modification = modification(20000001, -1, true);
        CashItemInfo commodity = new CashItemInfo(
                2000000, 1, 100, 20000001, 0, 2, false);

        factory.loadModifications(Arrays.asList(modification), Arrays.asList(commodity));

        assertNull(factory.getModInfo(20000001));
    }

    @Test
    public void excludesMesoPricedCashShopModifications() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem modification = modification(10000001, -1, true);
        modification.meso = 100;
        CashItemInfo commodity = new CashItemInfo(
                1001000, 1, 780, 10000001, 0, 2, false);

        factory.loadModifications(Arrays.asList(modification), Arrays.asList(commodity));

        assertNull(factory.getModInfo(10000001));
    }

    @Test
    public void omitsOverridesThatMatchCommodityData() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem modification = modification(20000001, -1, true);
        modification.discountPrice = 500;
        modification.priority = 0;
        modification.period = 30;
        modification.gender = 2;
        modification.count = 1;
        CashItemInfo commodity = new CashItemInfo(
                2000000, 1, 500, 20000001, 30, 2, false);

        factory.loadModifications(Arrays.asList(modification), Arrays.asList(commodity));

        assertEquals(0x400, factory.getModInfo(20000001).flags);
    }

    @Test
    public void doesNotSerializeTheDefaultZeroCatalogMark() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem modification = modification(20000001, 0, true);
        CashItemInfo commodity = new CashItemInfo(
                2000000, 1, 500, 20000001, 30, 2, false);

        factory.loadModifications(Arrays.asList(modification), Arrays.asList(commodity));

        assertEquals(0x400, factory.getModInfo(20000001).flags);
    }

    @Test
    public void retainsPriceOverrideThatDiffersFromCommodityData() {
        CashItemFactory factory = new CashItemFactory();
        DCashShopModifiedItem modification = modification(20000001, -1, true);
        modification.discountPrice = 300;
        CashItemInfo commodity = new CashItemInfo(
                2000000, 1, 500, 20000001, 0, 2, false);

        factory.loadModifications(Arrays.asList(modification), Arrays.asList(commodity));

        assertEquals(300, factory.getModInfo(20000001).discountPrice);
        assertTrue((factory.getModInfo(20000001).flags & 0x4) != 0);
    }

    private static DCashShopModifiedItem modification(int serial, int mark, boolean showUp) {
        DCashShopModifiedItem item = new DCashShopModifiedItem();
        item.serial = serial;
        item.name = "test";
        item.discountPrice = 0;
        item.mark = mark;
        item.showup = showUp;
        item.itemid = 0;
        item.priority = -1;
        item.packageField = false;
        item.period = 0;
        item.gender = -1;
        item.count = 0;
        item.meso = 0;
        item.unk1 = 0;
        item.unk2 = 0;
        item.unk3 = 0;
        item.extraFlags = 0;
        return item;
    }
}
