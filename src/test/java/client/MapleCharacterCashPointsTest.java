package client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MapleCharacterCashPointsTest {

    @Test
    public void rejectsBalancesAboveTheClientSafeLimit() {
        assertEquals(1_000_000_000,
                MapleCharacter.calculateCashPointBalance(1_000_000_000, 1));
    }

    @Test
    public void rejectsSpendingBelowZero() {
        assertEquals(100, MapleCharacter.calculateCashPointBalance(100, -101));
    }

    @Test
    public void appliesValidPurchase() {
        assertEquals(720, MapleCharacter.calculateCashPointBalance(2000, -1280));
    }
}
