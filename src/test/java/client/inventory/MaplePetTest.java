package client.inventory;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaplePetTest {

    @Test
    public void mapsAbilityItemsWithoutGrantingOtherCapabilities() throws Exception {
        MaplePet.PetFlag[] expected = new MaplePet.PetFlag[]{
                MaplePet.PetFlag.ITEM_PICKUP,
                MaplePet.PetFlag.HP_CHARGE,
                MaplePet.PetFlag.EXPAND_PICKUP,
                MaplePet.PetFlag.AUTO_PICKUP,
                MaplePet.PetFlag.LEFTOVER_PICKUP,
                MaplePet.PetFlag.UNPICKABLE,
                MaplePet.PetFlag.MP_CHARGE,
                MaplePet.PetFlag.PET_RECALL,
                MaplePet.PetFlag.PET_AUTO_SPEAKING
        };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], MaplePet.PetFlag.getByAddId(5190000 + i));
        }
        assertTrue(MaplePet.PetFlag.UNPICKABLE.isSupportedByClient());
        assertTrue(MaplePet.PetFlag.PET_RECALL.isSupportedByClient());
        assertTrue(MaplePet.PetFlag.PET_AUTO_SPEAKING.isSupportedByClient());

        Constructor<MaplePet> constructor = MaplePet.class.getDeclaredConstructor(int.class, int.class);
        constructor.setAccessible(true);
        MaplePet pet = constructor.newInstance(5000004, 999999);
        pet.setFlags(0x3FF);
        assertEquals(0x1FF, pet.getClientFlags());

        int flags = MaplePet.PetFlag.HP_CHARGE.getValue();
        assertTrue(MaplePet.PetFlag.HP_CHARGE.check(flags));
        assertFalse(MaplePet.PetFlag.ITEM_PICKUP.check(flags));
    }
}
