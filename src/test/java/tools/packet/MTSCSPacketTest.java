package tools.packet;

import client.inventory.Item;
import client.inventory.MaplePet;
import com.github.mrzhqiang.maplestory.config.ServerProperties;
import constants.ServerConstants;
import handling.MaplePacket;
import handling.SendPacketOpcode;
import org.junit.BeforeClass;
import org.junit.Test;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MTSCSPacketTest {

    @BeforeClass
    public static void setUpServerProperties() {
        ServerConstants.properties = new ServerProperties(new Properties());
    }

    @Test
    public void confirmFromCashShopUsesServerAssignedInventorySlot() {
        Item item = new Item(5010013, (short) 9, (short) 1, (byte) 0, 133);
        item.setExpiration(System.currentTimeMillis() + 86400000L);

        MaplePacket packet = MTSCSPacket.confirmFromCSInventory(item, (short) 9);
        byte[] bytes = packet.getBytes();

        assertEquals(SendPacketOpcode.CS_OPERATION.getValue(), readShort(bytes, 0));
        assertEquals(0x5D, bytes[2] & 0xFF);
        assertEquals(9, readShort(bytes, 3));
        assertEquals(item.getType(), bytes[5] & 0xFF);
    }

    @Test
    public void cashShopPacketTrimmingPreservesPackagesAndCatalogTail() {
        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.writeZeroBytes(5);
        int catalogOffset = writer.size();
        writer.writeInt(0);
        writer.writeShort(3);
        writeCatalogEntry(writer, 60000001, 0x800);
        writeCatalogEntry(writer, 70000123, 0x800);
        writeCatalogEntry(writer, 60000002, 0x4);
        writer.write(new byte[]{0x55, 0x66, 0x77});
        byte[] original = writer.toByteArray();

        byte[] fitted = MTSCSPacket.fitCashShopOpenPacket(original, catalogOffset, original.length - 9);

        assertEquals(original.length - 9, fitted.length);
        assertEquals(2, readShort(fitted, catalogOffset + 4));
        assertEquals(70000123, readInt(fitted, catalogOffset + 6));
        assertEquals(60000002, readInt(fitted, catalogOffset + 15));
        assertArrayEquals(new byte[]{0x55, 0x66, 0x77},
                new byte[]{fitted[fitted.length - 3], fitted[fitted.length - 2], fitted[fitted.length - 1]});
    }

    @Test
    public void cashShopPacketBelowLimitIsNotRewritten() {
        byte[] packet = new byte[]{1, 2, 3};

        assertArrayEquals(packet, MTSCSPacket.fitCashShopOpenPacket(packet, 0, packet.length));
    }

    @Test
    public void v079PetInventoryEntryKeepsReservedTailZeroed() throws Exception {
        Item item = new Item(5000004, (short) 11, (short) 1, (byte) 2, 145);
        item.setExpiration(System.currentTimeMillis() + 86400000L);
        MaplePet pet = newPet(5000004, 145, (short) 11);
        pet.setFlags(23);
        item.setPet(pet);
        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();

        PacketHelper.addItemInfo(writer, item, false, false);
        byte[] bytes = writer.toByteArray();

        assertEquals(58, bytes.length);
        assertEquals(3, bytes[1] & 0xFF);
        assertEquals(5000004, readInt(bytes, 2));
        assertEquals(145, readInt(bytes, 7));
        assertArrayEquals(new byte[10], Arrays.copyOfRange(bytes, bytes.length - 10, bytes.length));
    }

    private static MaplePet newPet(int itemId, int uniqueId, short inventoryPosition) throws Exception {
        Constructor<MaplePet> constructor = MaplePet.class.getDeclaredConstructor(
                int.class, int.class, short.class);
        constructor.setAccessible(true);
        MaplePet pet = constructor.newInstance(itemId, uniqueId, inventoryPosition);
        pet.setName("Pet");
        pet.setLevel(1);
        pet.setCloseness(1);
        pet.setFullness(100);
        return pet;
    }

    private static void writeCatalogEntry(MaplePacketLittleEndianWriter writer, int sn, int flags) {
        writer.writeInt(sn);
        writer.writeInt(flags);
        if ((flags & 0x4) != 0) {
            writer.writeInt(1234);
        }
        if ((flags & 0x800) != 0) {
            writer.write(1);
        }
    }

    private static int readShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static int readInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }
}
