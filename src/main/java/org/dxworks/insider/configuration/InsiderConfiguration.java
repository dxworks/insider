package org.dxworks.insider.configuration;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.argumenthor.Argumenthor;
import org.dxworks.argumenthor.config.ArgumenthorConfiguration;
import org.dxworks.argumenthor.config.fields.impl.NumberField;
import org.dxworks.argumenthor.config.fields.impl.StringField;
import org.dxworks.argumenthor.config.fields.impl.StringListField;
import org.dxworks.argumenthor.config.sources.impl.EnvSource;
import org.dxworks.argumenthor.config.sources.impl.PropertiesSource;
import org.dxworks.insider.technology.finder.LinguistService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.dxworks.insider.constants.InsiderConstants.*;

@Slf4j
public class InsiderConfiguration {

    private static InsiderConfiguration _instance = new InsiderConfiguration();
    private static Argumenthor argumenthor;

    private String insiderVersion = null;
    private String projectID = null;
    private String rootFolder = null;
    private List<String> languages = null;
    private String languagesFile = null;
    private int depextMaxNamespaceLength = -1;

    private InsiderConfiguration() {
    }

    public static InsiderConfiguration getInstance() {
        return _instance;
    }

    public void load() {

        System.out.println("Reading configuration");
        readInsiderVersion();

        ArgumenthorConfiguration argumenthorConfiguration = new ArgumenthorConfiguration(
                new StringField(PROJECT_ID, null),
                new StringField(ROOT_FOLDER, "."),
                new StringListField(LANGUAGES, List.of(), ","),
                new StringField(LINGUIST_FILE, DEFAULT_LINGUIST_FILE),
                new NumberField(DEPEXT_MAX_NAMESPACE_LENGTH, DEFAULT_DEPEXT_MAX_NAMESPACE_LENGTH)
        );

        PropertiesSource propertiesSource = new PropertiesSource();
        propertiesSource.setPath("config/insider-conf.properties");
        argumenthorConfiguration.addSource(new EnvSource("INSIDER"));
        argumenthorConfiguration.addSource(propertiesSource);
        argumenthor = new Argumenthor(argumenthorConfiguration);

        LinguistService.getInstance().initLinguist(getLanguagesFile());

        System.out.println("Insider " + getInsiderVersion());
        System.out.println("Project ID: " + getProjectID());
        System.out.println("Root Folder: " + getRootFolder());
        System.out.println("Languages: " + getLanguages());
    }

    public void readInsiderVersion() {
        try {
            insiderVersion = new String(getClass().getClassLoader().getResourceAsStream("insider-version").readAllBytes()).trim();
        } catch (IOException e) {
            log.warn("Could not read Insider Version", e);
        }
    }

    public String getRootFolder() {
        if (rootFolder == null) {
            rootFolder = (String) argumenthor.getRawValue(ROOT_FOLDER);
            if (rootFolder.endsWith("\\") || rootFolder.endsWith("/")) {
                rootFolder = rootFolder.substring(0, rootFolder.length() - 1);
            }
        }

        return rootFolder;
    }

    public String getProjectID() {
        if (projectID == null) {
            projectID = (String) argumenthor.getRawValue(PROJECT_ID);
        }
        return projectID;
    }

    public List<String> getLanguages() {
        if (languages == null) {
            languages = ((List<String>) argumenthor.getRawValue(LANGUAGES)).stream()
                    .filter(s -> !s.isBlank()).collect(Collectors.toList());
        }
        return languages;
    }

    public String getLanguagesFile() {
        if (languagesFile == null) {
            languagesFile = (String) argumenthor.getRawValue(LINGUIST_FILE);
        }
        return languagesFile;
    }

    public int getDepextMaxNamespaceLength() {
        if (depextMaxNamespaceLength == -1) {
            try {
            depextMaxNamespaceLength = (int) argumenthor.getRawValue(DEPEXT_MAX_NAMESPACE_LENGTH);
            }
            catch (Exception e) {
                depextMaxNamespaceLength = DEFAULT_DEPEXT_MAX_NAMESPACE_LENGTH;
            }
        }

        return depextMaxNamespaceLength;
    }

    public String getInsiderVersion() {
        return insiderVersion;
    }
}
