package server;

import com.github.mrzhqiang.maplestory.wz.WzData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CashItemFactoryTest {

    @BeforeClass
    public static void loadWzData() {
        WzData.load();
    }

    @Test
    public void readsNestedPackageSerialNumbers() {
        CashItemFactory factory = new CashItemFactory();

        assertEquals(Arrays.asList(20000155, 20400079, 20500079,
                        20600044, 20700007, 21100015),
                factory.getPackageSerialNumbers(9100000));
        assertEquals(Arrays.asList(50400007, 50400007, 50400007, 50400007, 50400007,
                        50400007, 50400007, 50400007, 50400007, 50400007),
                factory.getPackageSerialNumbers(9101289));
    }
}
