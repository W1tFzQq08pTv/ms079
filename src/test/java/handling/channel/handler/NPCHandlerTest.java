package handling.channel.handler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NPCHandlerTest {

    @Test
    public void normalizesEquipmentQuantitySentAsUnsignedShortMaximum() {
        assertEquals(1, NPCHandler.normalizeSellQuantity((short) 0xFFFF));
    }

    @Test
    public void normalizesZeroSellQuantity() {
        assertEquals(1, NPCHandler.normalizeSellQuantity((short) 0));
    }

    @Test
    public void preservesStackedItemSellQuantity() {
        assertEquals(200, NPCHandler.normalizeSellQuantity((short) 200));
    }
}
