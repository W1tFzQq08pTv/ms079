package tools.packet;

import org.junit.Test;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;

public class MTSCSPacketTest {

    @Test
    public void cashShopCatalogTailMatchesV079Layout() {
        int[] bestItems = {50400041, 50400016, 50400135, 50400061, 20800204};
        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();

        MTSCSPacket.addCashShopCatalogTail(writer, bestItems);

        byte[] bytes = HexTool.getByteArrayFromHexString(writer.toString());
        assertEquals(123 + (8 * 2 * 5 * 12) + 5, bytes.length);

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(123);
        assertEquals(1, buffer.getInt());
        assertEquals(0, buffer.getInt());
        assertEquals(50400041, buffer.getInt());

        buffer.position(123 + (8 * 2 * 5 * 12));
        while (buffer.hasRemaining()) {
            assertEquals(0, buffer.get());
        }
    }
}
