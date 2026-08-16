package tools.packet;

import org.junit.Test;
import server.CashItemInfo.CashModInfo;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
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

    @Test
    public void legacyActivityCatalogMatchesCapturedV079Layout() throws NoSuchAlgorithmException {
        byte[] catalog = MTSCSPacket.legacyActivityCatalog();

        assertEquals(MTSCSPacket.LEGACY_ACTIVITY_CATALOG_LENGTH, catalog.length);
        assertEquals("0c7a0d1b3ea7f6c4afc7c79e81dd8f3ad7a94b9875f51a2eb38bf5f4bb415dc0",
                sha256(catalog));

        ByteBuffer buffer = ByteBuffer.wrap(catalog).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(70, buffer.getInt());
        buffer.position(buffer.position() + (70 * Integer.BYTES));
        assertEquals(2902, buffer.getShort() & 0xFFFF);
        for (int index = 0; index < 2902; index++) {
            skipCashModification(buffer);
        }
        assertEquals(28276, buffer.position());

        MaplePacketLittleEndianWriter tailWriter = new MaplePacketLittleEndianWriter();
        MTSCSPacket.addCashShopCatalogTail(tailWriter,
                new int[]{50400041, 50400016, 50400135, 50400061, 20800204});
        byte[] expectedTail = tailWriter.toByteArray();
        byte[] actualTail = new byte[buffer.remaining()];
        buffer.get(actualTail);
        assertEquals(1088, actualTail.length);
        assertArrayEquals(expectedTail, actualTail);
    }

    @Test
    public void cashShopCatalogSamplesEveryCategoryAndSerializesInStableOrder() {
        int[] bestItems = {50400041, 50400016, 50400135, 50400061, 20800204};
        List<CashModInfo> modifications = new ArrayList<CashModInfo>();
        CashModInfo firstCategoryFirst = showUpModification(10);
        firstCategoryFirst.catalogBucket = 1;
        modifications.add(firstCategoryFirst);
        CashModInfo firstCategorySecond = showUpModification(20);
        firstCategorySecond.catalogBucket = 1;
        modifications.add(firstCategorySecond);
        CashModInfo firstCategoryThird = showUpModification(30);
        firstCategoryThird.catalogBucket = 1;
        modifications.add(firstCategoryThird);
        CashModInfo secondCategory = showUpModification(40);
        secondCategory.catalogBucket = 2;
        modifications.add(secondCategory);

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        int fixedCatalogBytes = 4 + (70 * 4) + 2 + 123 + (8 * 2 * 5 * 12) + 5;
        int written = MTSCSPacket.addCashShopCatalog(writer, modifications, bestItems,
                fixedCatalogBytes + (3 * 9));

        assertEquals(3, written);
        ByteBuffer buffer = ByteBuffer.wrap(writer.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(70, buffer.getInt());
        buffer.position(buffer.position() + (70 * 4));
        assertEquals(3, buffer.getShort() & 0xFFFF);
        assertEquals(10, readShowUpModification(buffer));
        assertEquals(20, readShowUpModification(buffer));
        assertEquals(40, readShowUpModification(buffer));
    }

    @Test
    public void cashShopCatalogHonorsConfiguredItemLimit() {
        int[] bestItems = {50400041, 50400016, 50400135, 50400061, 20800204};
        List<CashModInfo> modifications = new ArrayList<CashModInfo>();
        modifications.add(showUpModification(10));
        modifications.add(showUpModification(20));
        modifications.add(showUpModification(30));

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        int written = MTSCSPacket.addCashShopCatalog(writer, modifications, bestItems, 4096, 2);

        assertEquals(2, written);
        ByteBuffer buffer = ByteBuffer.wrap(writer.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(70, buffer.getInt());
        buffer.position(buffer.position() + (70 * 4));
        assertEquals(2, buffer.getShort() & 0xFFFF);
        assertEquals(10, readShowUpModification(buffer));
        assertEquals(20, readShowUpModification(buffer));
    }

    @Test
    public void cashShopCatalogFiltersToConfiguredSerials() {
        List<CashModInfo> modifications = new ArrayList<CashModInfo>();
        modifications.add(showUpModification(10));
        modifications.add(showUpModification(20));
        modifications.add(showUpModification(30));
        Set<Integer> configuredSerials = new HashSet<Integer>(Arrays.asList(10, 30, 99));

        Collection<CashModInfo> selected = MTSCSPacket.filterCashShopCatalog(modifications, configuredSerials);

        List<Integer> selectedSerials = new ArrayList<Integer>();
        for (CashModInfo modification : selected) {
            selectedSerials.add(modification.sn);
        }
        assertEquals(Arrays.asList(10, 30), selectedSerials);
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

    private static void skipCashModification(ByteBuffer buffer) {
        buffer.getInt();
        int flags = buffer.getInt();
        skip(buffer, (flags & 0x1) != 0 ? Integer.BYTES : 0);
        skip(buffer, (flags & 0x2) != 0 ? Short.BYTES : 0);
        skip(buffer, (flags & 0x4) != 0 ? Integer.BYTES : 0);
        skip(buffer, (flags & 0x8) != 0 ? 1 : 0);
        skip(buffer, (flags & 0x10) != 0 ? 1 : 0);
        skip(buffer, (flags & 0x20) != 0 ? Short.BYTES : 0);
        skip(buffer, (flags & 0x40) != 0 ? Integer.BYTES : 0);
        skip(buffer, (flags & 0x80) != 0 ? Integer.BYTES : 0);
        skip(buffer, (flags & 0x100) != 0 ? 1 : 0);
        skip(buffer, (flags & 0x200) != 0 ? 1 : 0);
        skip(buffer, (flags & 0x400) != 0 ? 1 : 0);
        skip(buffer, (flags & 0x800) != 0 ? 1 : 0);
        skip(buffer, (flags & 0x1000) != 0 ? 1 : 0);
        skip(buffer, (flags & 0x2000) != 0 ? Short.BYTES : 0);
        skip(buffer, (flags & 0x4000) != 0 ? Short.BYTES : 0);
        skip(buffer, (flags & 0x8000) != 0 ? Short.BYTES : 0);
        if ((flags & 0x10000) != 0) {
            skip(buffer, (buffer.get() & 0xFF) * Integer.BYTES);
        }
    }

    private static void skip(ByteBuffer buffer, int count) {
        buffer.position(buffer.position() + count);
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(digest.length * 2);
        for (byte current : digest) {
            value.append(Character.forDigit((current >>> 4) & 0xF, 16));
            value.append(Character.forDigit(current & 0xF, 16));
        }
        return value.toString();
    }
}
