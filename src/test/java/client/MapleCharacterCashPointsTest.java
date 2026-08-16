package client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MapleCharacterCashPointsTest {

    @Test
    public void rejectsBalancesAboveTheClientSafeLimit() {
        assertEquals(999_999_999,
                MapleCharacter.calculateCashPointBalance(999_999_999, 1));
    }

    @Test
    public void clampsExistingTenDigitBalancesForClientPackets() {
        assertEquals(999_999_999,
                MapleCharacter.clientSafeCashPointBalance(1_000_000_000));
    }

    @Test
    public void clampsInvalidNegativeBalancesForClientPackets() {
        assertEquals(0, MapleCharacter.clientSafeCashPointBalance(-1));
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
