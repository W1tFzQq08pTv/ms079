package tools.packet;

import org.junit.Test;
import server.CashItemInfo.CashModInfo;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MTSCSPacketTest {

    private static final int MAPLE_PACKET_HEADER_LENGTH = 4;

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

    @Test
    public void cashShopCatalogStaysWithinPacketLimitAndUsesStableSerialOrder() {
        int[] bestItems = {50400041, 50400016, 50400135, 50400061, 20800204};
        List<CashModInfo> modifications = new ArrayList<CashModInfo>();
        modifications.add(showUpModification(30));
        modifications.add(showUpModification(10));
        modifications.add(showUpModification(40));
        modifications.add(showUpModification(20));

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.writeZeroBytes(16);
        int fixedCatalogBytes = 4 + (70 * 4) + 2 + 123 + (8 * 2 * 5 * 12) + 5;
        int maxPacketLength = 16 + fixedCatalogBytes + (3 * 9);

        int written = MTSCSPacket.addCashShopCatalog(writer, modifications, bestItems, maxPacketLength);

        byte[] bytes = writer.toByteArray();
        assertEquals(3, written);
        assertTrue(bytes.length <= maxPacketLength);

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(16);
        assertEquals(70, buffer.getInt());
        buffer.position(buffer.position() + (70 * 4));
        assertEquals(3, buffer.getShort() & 0xFFFF);
        assertEquals(10, readShowUpModification(buffer));
        assertEquals(20, readShowUpModification(buffer));
        assertEquals(30, readShowUpModification(buffer));
    }

    @Test
    public void cashShopPayloadLeavesRoomForEncryptedPacketHeader() {
        assertEquals(Short.MAX_VALUE,
                MTSCSPacket.MAX_MAPLE_PACKET_PAYLOAD_LENGTH + MAPLE_PACKET_HEADER_LENGTH);
    }

    private static CashModInfo showUpModification(int serial) {
        return new CashModInfo(serial, 0, -2, true, 0, -1,
                false, 0, -1, 0, 0, 0, 0, 0, 0);
    }

    private static int readShowUpModification(ByteBuffer buffer) {
        int serial = buffer.getInt();
        assertEquals(0x400, buffer.getInt());
        assertEquals(1, buffer.get());
        return serial;
    }
}
