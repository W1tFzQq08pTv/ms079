package handling.channel.handler;

import org.junit.Test;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerHandlerTest {

    @Test
    public void readsWheelFlagAfterTickAndReservedByte() {
        SeekableLittleEndianAccessor slea = accessor(
                0x78, 0x56, 0x34, 0x12,
                0x00,
                0x01, 0x00);

        assertTrue(PlayerHandler.readWheelRequested(slea));
        assertEquals(0, slea.available());
    }

    @Test
    public void readsLegacyTwoByteWheelFlag() {
        SeekableLittleEndianAccessor slea = accessor(0x01, 0x00);

        assertTrue(PlayerHandler.readWheelRequested(slea));
        assertEquals(0, slea.available());
    }

    @Test
    public void treatsMissingWheelFlagAsFalse() {
        SeekableLittleEndianAccessor slea = accessor();

        assertFalse(PlayerHandler.readWheelRequested(slea));
        assertEquals(0, slea.available());
    }

    private static SeekableLittleEndianAccessor accessor(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(bytes));
    }
}
