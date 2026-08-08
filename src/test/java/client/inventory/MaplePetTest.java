package client.inventory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MaplePetTest {

    @Test
    public void addsAllPickupCapabilitiesAndPreservesExistingCapabilities() {
        assertEquals(55, MaplePet.withFullPickupFlags(MaplePet.PetFlag.HP_CHARGE.getValue()));
        assertEquals(119, MaplePet.withFullPickupFlags(
                MaplePet.PetFlag.HP_CHARGE.getValue() | MaplePet.PetFlag.MP_CHARGE.getValue()));
    }
}
