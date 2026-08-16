package server;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CashItemInfoTest {

    @Test
    public void showOnlyModificationUsesOnlyShowFlag() {
        CashItemInfo.CashModInfo modification = new CashItemInfo.CashModInfo(
                10000001, 0, -2, true, 0, -1,
                false, 0, -1, 0, 0, 0, 0, 0, 0);

        assertEquals(0x400, modification.flags);
    }

    @Test
    public void negativeOneMarkOmitsProductBadgeOverride() {
        CashItemInfo.CashModInfo modification = new CashItemInfo.CashModInfo(
                10000001, 0, -1, true, 0, -1,
                false, 0, -1, 0, 0, 0, 0, 0, 0);

        assertEquals(0x400, modification.flags);
        assertEquals(-1, modification.mark);
    }

    @Test
    public void commodityItemIdWinsOverStaleDatabaseOverride() {
        CashItemInfo commodity = new CashItemInfo(1302016, 1, 500, 80000001, 0, 2, false);
        CashItemInfo.CashModInfo modification = new CashItemInfo.CashModInfo(
                80000001, 100, -1, true, 9999999, -1,
                false, 0, -1, 0, 0, 0, 0, 0, 0);

        CashItemInfo purchasable = modification.toCItem(commodity);

        assertEquals(1302016, purchasable.getId());
        assertEquals(100, purchasable.getPrice());
    }
}
