package com.github.mrzhqiang.maplestory.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

/**
 * 服务端配置。
 */
public final class ServerConfiguration extends AbstractModule {

    public static final ServerConfiguration INSTANCE = new ServerConfiguration();

    private ServerConfiguration() {
        // inner instance
    }

    private static final String DEFAULT_CONFIG_FILE = "config/server.properties";
    private static final String CONFIG_FILE_PROPERTY = "ms079.config";
    private static final String CONFIG_FILE_ENVIRONMENT = "MS079_CONFIG_FILE";

    /**
     * 主要是提供给数据库配置使用，其他地方暂时用不到。
     */
    @Named("config")
    @Singleton
    @Provides
    static Properties provideProperties() {
        Properties properties = new Properties();
        String configFile = resolveConfigFile();
        try (Reader reader = new FileReader(configFile)) {
            properties.load(reader);
        } catch (Exception e) {
            throw new RuntimeException(String.format("加载 %s 文件出现问题。", configFile), e);
        }
        return properties;
    }

    static String resolveConfigFile() {
        String configured = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(CONFIG_FILE_ENVIRONMENT);
        }
        return configured == null || configured.trim().isEmpty() ? DEFAULT_CONFIG_FILE : configured;
    }

}
