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
}
