package client;

import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import com.github.mrzhqiang.maplestory.config.ServerProperties;
import com.github.mrzhqiang.maplestory.wz.WzData;
import constants.ServerConstants;
import handling.MaplePacket;
import handling.SendPacketOpcode;
import org.apache.mina.core.future.WriteFuture;
import org.junit.BeforeClass;
import org.junit.Test;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.MockIOSession;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapleCharacterPetRestoreTest {

    @BeforeClass
    public static void loadWzData() {
        ServerConstants.properties = new ServerProperties(new Properties());
        WzData.load();
    }

    @Test
    public void restoresSavedAutoPotionSelections() throws Exception {
        CapturingSession session = new CapturingSession();
        MapleCharacter character = newCharacter(session);
        character.getQuestNAdd(MapleQuest.getInstance(122221)).setCustomData("2022174");
        character.getQuestNAdd(MapleQuest.getInstance(122222)).setCustomData("2000017");

        character.updatePetEquip();

        assertEquals(2, session.packets.size());
        assertPacket(session.packets.get(0), SendPacketOpcode.AUTO_HP_POT, 2022174);
        assertPacket(session.packets.get(1), SendPacketOpcode.AUTO_MP_POT, 2000017);
    }

    @Test
    public void restoresSavedPetWithInventoryActivationPacket() throws Exception {
        CapturingSession session = new CapturingSession();
        MapleCharacter character = newCharacter(session);
        MaplePet pet = newPet(5000020, 47, (short) 5);
        Item petItem = new Item(5000020, (short) 5, (short) 1, (byte) 0, 47);
        petItem.setPet(pet);
        petItem.setExpiration(System.currentTimeMillis() + 86400000L);
        character.getInventory(MapleInventoryType.CASH).addFromDB(petItem);
        character.addPet(pet);
        character.getPetStores()[0] = 5;

        character.spawnSavedPets();

        assertTrue(pet.getSummoned());
        assertTrue(session.hasOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM));
        assertEquals(-1, character.getPetStores()[0]);
    }

    @Test
    public void restoresEveryPersistedPetCapabilityAfterSpawn() throws Exception {
        CapturingSession session = new CapturingSession();
        MapleCharacter character = newCharacter(session);
        MaplePet pet = newPet(5000004, 145, (short) 8);
        pet.setFlags(0x1FF);
        Item petItem = new Item(5000004, (short) 8, (short) 1, (byte) 2, 145);
        petItem.setPet(pet);
        petItem.setExpiration(System.currentTimeMillis() + 86400000L);
        character.getInventory(MapleInventoryType.CASH).addFromDB(petItem);
        character.addPet(pet);
        character.getPetStores()[0] = 8;

        character.syncPetFlagsAfterMapReady();
        character.spawnSavedPets();

        assertEquals(0, session.countOpcode(SendPacketOpcode.PET_FLAG_CHANGE));

        character.syncPetFlagsAfterMapReady();
        character.syncPetFlagsAfterMapReady();

        assertEquals(9, session.countOpcode(SendPacketOpcode.PET_FLAG_CHANGE));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.ITEM_PICKUP.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.EXPAND_PICKUP.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.AUTO_PICKUP.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.LEFTOVER_PICKUP.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.HP_CHARGE.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.MP_CHARGE.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.UNPICKABLE.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.PET_RECALL.getValue()));
        assertTrue(session.hasPetFlag(145, MaplePet.PetFlag.PET_AUTO_SPEAKING.getValue()));

        character.removePet(pet, true);
        pet.setSummoned(8);
        character.syncPetFlagsAfterMapReady();

        assertEquals(18, session.countOpcode(SendPacketOpcode.PET_FLAG_CHANGE));
    }

    private static MapleCharacter newCharacter(CapturingSession session) throws Exception {
        Constructor<MapleCharacter> constructor = MapleCharacter.class.getDeclaredConstructor(boolean.class);
        constructor.setAccessible(true);
        MapleCharacter character = constructor.newInstance(true);
        MapleClient client = new MapleClient(null, null, session);
        client.setPlayer(character);
        character.setClient(client);
        character.setMap(new MapleMap(100000000, 1, 100000000, 1.0f));
        return character;
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

    private static void assertPacket(MaplePacket packet, SendPacketOpcode opcode, int itemId) {
        byte[] bytes = packet.getBytes();
        assertEquals(opcode.getValue(), readShort(bytes, 0));
        assertEquals(itemId, readInt(bytes, 2));
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

    private static final class CapturingSession extends MockIOSession {

        private final List<MaplePacket> packets = new ArrayList<MaplePacket>();

        @Override
        public WriteFuture write(Object message) {
            if (message instanceof MaplePacket) {
                packets.add((MaplePacket) message);
            }
            return null;
        }

        private boolean hasOpcode(SendPacketOpcode opcode) {
            for (MaplePacket packet : packets) {
                if (readShort(packet.getBytes(), 0) == opcode.getValue()) {
                    return true;
                }
            }
            return false;
        }

        private int countOpcode(SendPacketOpcode opcode) {
            int count = 0;
            for (MaplePacket packet : packets) {
                if (readShort(packet.getBytes(), 0) == opcode.getValue()) {
                    count++;
                }
            }
            return count;
        }

        private boolean hasPetFlag(int uniqueId, int flag) {
            for (MaplePacket packet : packets) {
                byte[] bytes = packet.getBytes();
                if (readShort(bytes, 0) == SendPacketOpcode.PET_FLAG_CHANGE.getValue()
                        && readInt(bytes, 2) == uniqueId
                        && (bytes[10] & 0xFF) == 1
                        && readShort(bytes, 11) == flag) {
                    return true;
                }
            }
            return false;
        }
    }
}
