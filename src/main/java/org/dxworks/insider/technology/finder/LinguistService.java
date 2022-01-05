package org.dxworks.insider.technology.finder;

import lombok.NonNull;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.configuration.InsiderConfiguration;
import org.dxworks.linguist.Language;
import org.dxworks.linguist.Linguist;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.dxworks.insider.constants.InsiderConstants.DEFAULT_LINGUIST_FILE;

public class LinguistService {

    private static final LinguistService _instance = new LinguistService();
    private Linguist linguist;
    private List<String> programmingLanguages;

    private LinguistService() {
    }

    public static LinguistService getInstance() {
        return _instance;
    }

    public boolean hasAcceptedExtension(InsiderFile insiderFile) {
        return hasAcceptedExtension(insiderFile.getPath());
    }

    public boolean hasAcceptedExtension(InsiderFile insiderFile, @NonNull List<String> languages) {
        return hasAcceptedExtension(insiderFile.getPath(), languages);
    }

    public boolean hasAcceptedExtension(String path) {
        return hasAcceptedExtension(path, InsiderConfiguration.getInstance().getLanguages());
    }

    public boolean hasAcceptedExtension(String path, @NonNull List<String> languages) {
        return linguist.isOf(path, languages.toArray(new String[0]));
    }

    public List<String> getLanguagesForFile(InsiderFile file) {
        return linguist.getLanguages(file.getPath()).stream()
            .map(Language::getName)
            .collect(Collectors.toList());
    }

    public boolean containsLanguage(String lang) {
        return linguist.isRegistered(lang);
    }

    private List<String> getAllProgrammingLanguages() {
        return linguist.getLanguages().values().stream()
            .filter(lang -> "programming".equalsIgnoreCase(lang.getType()))
            .map(Language::getName)
            .collect(Collectors.toList());
    }


    public List<String> getProgrammingLanguages() {
        return programmingLanguages;
    }

    public void initLinguist(String linguistFile) {
        if (linguistFile == null) {
            try {
                linguist = new Linguist();
            } catch (Exception e) {
                linguist = new Linguist(Path.of(DEFAULT_LINGUIST_FILE).toFile());
            }
        } else {
            linguist = new Linguist(Path.of(linguistFile).toFile());
        }
        programmingLanguages = getAllProgrammingLanguages();
    }

}
