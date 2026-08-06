package com.github.mrzhqiang.maplestory.validation;

import com.github.mrzhqiang.maplestory.config.ServerProperties;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RepositoryDataValidationTest {

    private static final int MAX_REPORTED_FAILURES = 25;

    @Test
    public void compilesAllGameScriptsWithJava8Engine() throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        assertNotNull("Java 8 JavaScript engine is unavailable", engine);
        assertTrue("JavaScript engine does not support compilation", engine instanceof Compilable);

        Compilable compilable = (Compilable) engine;
        List<Path> scripts = repositoryFiles(Paths.get("脚本"), ".js");
        assertFalse("No game scripts were found", scripts.isEmpty());

        List<String> failures = new ArrayList<>();
        for (Path script : scripts) {
            try (Reader reader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
                compilable.compile(reader);
            } catch (AssertionError e) {
                addFailure(failures, script, e);
            } catch (Exception e) {
                addFailure(failures, script, e);
            }
        }
        assertNoFailures("JavaScript compilation", failures);
    }

    @Test
    public void parsesAllWzXmlWithoutExternalEntities() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setContentHandler(new DefaultHandler());
        reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

        List<Path> xmlFiles = repositoryFiles(Paths.get("wz"), ".xml");
        assertFalse("No WZ XML files were found", xmlFiles.isEmpty());

        List<String> failures = new ArrayList<>();
        for (Path xmlFile : xmlFiles) {
            try (InputStream input = Files.newInputStream(xmlFile)) {
                InputSource source = new InputSource(input);
                source.setSystemId(xmlFile.toUri().toString());
                reader.parse(source);
            } catch (Exception e) {
                addFailure(failures, xmlFile, e);
            }
        }
        assertNoFailures("WZ XML parsing", failures);
    }

    @Test
    public void validatesServerConfiguration() throws Exception {
        Path config = Paths.get("服务端配置.ini");
        assertTrue("服务端配置.ini is missing", Files.isRegularFile(config));

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        List<String> requiredKeys = Arrays.asList(
                "datasource.driver",
                "datasource.url",
                "datasource.username",
                "datasource.password",
                "server.name",
                "server.address",
                "server.login.port",
                "server.channel.port",
                "server.channel.count",
                "server.mall.port",
                "server.world.rate.exp",
                "server.world.rate.gold",
                "server.world.rate.drop",
                "server.world.rate.drop.boss",
                "server.world.rate.cash",
                "server.world.mob-respawn-interval",
                "server.world.mob-density-multiplier"
        );
        for (String key : requiredKeys) {
            assertTrue("Missing configuration key: " + key, properties.containsKey(key));
            assertFalse("Configuration value is empty: " + key,
                    properties.getProperty(key, "").trim().isEmpty());
        }

        assertTrue("datasource.url must use MySQL",
                properties.getProperty("datasource.url").startsWith("jdbc:mysql://"));

        int loginPort = integerInRange(properties, "server.login.port", 1, 65535);
        int channelPort = integerInRange(properties, "server.channel.port", 1, 65535);
        int channelCount = integerInRange(properties, "server.channel.count", 1, 100);
        int mallPort = integerInRange(properties, "server.mall.port", 1, 65535);
        assertTrue("Channel ports exceed 65535", channelPort + channelCount <= 65535);
        assertTrue("Server ports must be distinct", loginPort != mallPort && loginPort != channelPort && mallPort != channelPort);

        integerInRange(properties, "server.world.rate.exp", 1, Integer.MAX_VALUE);
        integerInRange(properties, "server.world.rate.gold", 1, Integer.MAX_VALUE);
        integerInRange(properties, "server.world.rate.drop", 1, Integer.MAX_VALUE);
        integerInRange(properties, "server.world.rate.drop.boss", 1, Integer.MAX_VALUE);
        integerInRange(properties, "server.world.rate.cash", 1, Integer.MAX_VALUE);
        integerInRange(properties, "server.world.mob-respawn-interval", 3000, Short.MAX_VALUE);
        floatInRange(properties, "server.world.mob-density-multiplier", 1.0f, 2.0f);

        assertNoDuplicateConfigurationKeys(config);
        new ServerProperties(properties);
    }

    private static List<Path> repositoryFiles(Path root, String suffix) throws IOException {
        assertTrue("Repository directory is missing: " + root, Files.isDirectory(root));
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static void assertNoDuplicateConfigurationKeys(Path config) throws IOException {
        Set<String> keys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim();
            if (!keys.add(key)) {
                duplicates.add(key);
            }
        }
        assertTrue("Duplicate configuration keys: " + duplicates, duplicates.isEmpty());
    }

    private static int integerInRange(Properties properties, String key, int min, int max) {
        final int value;
        try {
            value = Integer.parseInt(properties.getProperty(key).trim());
        } catch (NumberFormatException e) {
            throw new AssertionError("Configuration value is not an integer: " + key, e);
        }
        assertTrue(key + " must be between " + min + " and " + max, value >= min && value <= max);
        return value;
    }

    private static float floatInRange(Properties properties, String key, float min, float max) {
        final float value;
        try {
            value = Float.parseFloat(properties.getProperty(key).trim());
        } catch (NumberFormatException e) {
            throw new AssertionError("Configuration value is not a number: " + key, e);
        }
        assertTrue(key + " must be between " + min + " and " + max, value >= min && value <= max);
        return value;
    }

    private static void addFailure(List<String> failures, Path path, Throwable error) {
        if (failures.size() < MAX_REPORTED_FAILURES) {
            String message = error.getMessage();
            failures.add(path + ": " + error.getClass().getSimpleName()
                    + (message == null ? "" : " - " + message));
        }
    }

    private static void assertNoFailures(String operation, List<String> failures) {
        if (!failures.isEmpty()) {
            fail(operation + " failed; first " + failures.size() + " failure(s):\n"
                    + String.join("\n", failures));
        }
    }
}
