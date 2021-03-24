package org.dxworks.dxplatform.plugins.insider.configuration;

import org.dxworks.argumenthor.Argumenthor;
import org.dxworks.argumenthor.config.ArgumenthorConfiguration;
import org.dxworks.argumenthor.config.fields.impl.StringField;
import org.dxworks.argumenthor.config.fields.impl.StringListField;
import org.dxworks.argumenthor.config.sources.impl.EnvSource;
import org.dxworks.argumenthor.config.sources.impl.PropertiesSource;

import java.util.List;

import static org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants.*;

public class InsiderConfiguration {

    private static InsiderConfiguration _instance = new InsiderConfiguration();
    private static Argumenthor argumenthor;

    private InsiderConfiguration() {
    }

    public static InsiderConfiguration getInstance() {
        return _instance;
    }

    public void load() {

        System.out.println("Reading configuration: " + CONFIGURATION_FILE);

        ArgumenthorConfiguration argumenthorConfiguration = new ArgumenthorConfiguration(
                new StringField(PROJECT_ID, null),
                new StringField(ROOT_FOLDER, null),
                new StringListField(LANGUAGES, List.of(), ",")
        );

        PropertiesSource propertiesSource = new PropertiesSource();
        propertiesSource.setPath("config/insider-conf.properties");
        argumenthorConfiguration.addSource(new EnvSource());
        argumenthorConfiguration.addSource(propertiesSource);
        argumenthor = new Argumenthor(argumenthorConfiguration);

        System.out.println("Project ID: " + getProjectID());
        System.out.println("Root Folder: " + getRootFolder());
        System.out.println("Languages: " + getLanguages());
    }

    public String getRootFolder() {
        String rootFolder = (String) argumenthor.getRawValue(ROOT_FOLDER);
        if (rootFolder.endsWith("\\") || rootFolder.endsWith("/")) {
            rootFolder = rootFolder.substring(0, rootFolder.length() - 1);
        }

        return rootFolder;
    }

    public String getProjectID () {
        return (String) argumenthor.getRawValue(PROJECT_ID);
    }

    public List<String> getLanguages() {
        return (List<String>) argumenthor.getRawValue(LANGUAGES);
    }
}
