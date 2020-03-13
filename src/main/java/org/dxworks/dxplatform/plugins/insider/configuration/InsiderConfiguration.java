package org.dxworks.dxplatform.plugins.insider.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class InsiderConfiguration {

    private static InsiderConfiguration ourInstance = new InsiderConfiguration();
    private Properties config;

    private InsiderConfiguration() {
    }

    public static InsiderConfiguration getInstance() {
        return ourInstance;
    }

    public static void loadProperties(Properties properties) {
        if (ourInstance.config == null)
            ourInstance.config = properties;
    }

    public String getProperty(String property) {
        return config.getProperty(property);
    }

    public List<String> getListProperty(String property) {
        String propValue = config.getProperty(property);
        if (propValue == null)
            return Collections.emptyList();

        return Arrays.asList(propValue.split(","));
    }
}
