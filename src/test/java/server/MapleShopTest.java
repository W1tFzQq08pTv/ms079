package server;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapleShopTest {

    @Test
    public void acceptsMesoGainWithinIntegerLimit() {
        assertTrue(MapleShop.canReceiveMesos(1_000, 2_000));
    }

    @Test
    public void rejectsZeroMesoSale() {
        assertFalse(MapleShop.canReceiveMesos(1_000, 0));
    }

    @Test
    public void rejectsMesoGainBeyondIntegerLimit() {
        assertFalse(MapleShop.canReceiveMesos(Integer.MAX_VALUE - 10, 11));
    }
}
