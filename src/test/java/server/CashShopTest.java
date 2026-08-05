package server;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CashShopTest {

    @Test
    public void keepsConfiguredCashItemPeriod() {
        long now = 1_700_000_000_000L;

        assertEquals(now + 7L * 24L * 60L * 60L * 1000L,
                CashShop.expirationForPeriod(7, now));
        assertEquals(-1L, CashShop.expirationForPeriod(0, now));
    }
}
