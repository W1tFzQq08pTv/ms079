package com.github.mrzhqiang.maplestory.config;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

public class DatabasePropertiesTest {

    @Test
    public void testLoad() {
        Properties properties = new Properties();
        String path = Objects.requireNonNull(getClass().getResource("/test.ini")).getPath();
        try (FileReader reader = new FileReader(path)) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerProperties configuration = new ServerProperties(properties);
        Assert.assertEquals("兰达尔(v2021.1.0)", configuration.getName());
        Assert.assertEquals(100, configuration.getOnlineLimit());
    }

    @Test
    public void loadsConfiguredCashShopCatalogSerials() {
        Properties properties = new Properties();
        properties.setProperty("server.mall.catalog.serials", "10000001, 10000030, 10000001");
        properties.setProperty("server.mall.send-touching-cash-points", "false");

        ServerProperties configuration = new ServerProperties(properties);

        Assert.assertEquals(2, configuration.getCashShopCatalogSerials().size());
        Assert.assertTrue(configuration.getCashShopCatalogSerials().containsAll(
                Arrays.asList(10000001, 10000030)));
        Assert.assertFalse(configuration.isCashShopSendTouchingCashPoints());
    }
}
