package org.dxworks.dxplatform.plugins.insider.technology.finder;

import lombok.NonNull;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;
import org.dxworks.linguist.Language;
import org.dxworks.linguist.Linguist;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class LinguistService {

    private static final LinguistService _instance = new LinguistService();
    private Linguist linguist;
    private List<String> programmingLanguages;
    private String linguistFile;

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
        return getLinguist().isOf(path, languages.toArray(new String[0]));
    }

    public List<String> getLanguagesForFile(InsiderFile file) {
        return getLinguist().getLanguages(file.getPath()).stream()
                .map(Language::getName)
                .collect(Collectors.toList());
    }

    public boolean containsLanguage(String lang) {
        return getLinguist().isRegistered(lang);
    }

    private List<String> getAllProgrammingLanguages() {
        return getLinguist().getLanguages().values().stream()
                .filter(lang -> "programming".equalsIgnoreCase(lang.getType()))
                .map(Language::getName)
                .collect(Collectors.toList());
    }


    public List<String> getProgrammingLanguages() {
        return programmingLanguages;
    }

    public void setLinguistFile(String linguistFile) {
        this.linguistFile = linguistFile;
    }

    private Linguist getLinguist() {
        if (linguist == null) {
            linguist = linguistFile == null ? new Linguist() : new Linguist(Path.of(linguistFile).toFile());
            programmingLanguages = getAllProgrammingLanguages();
        }
        return linguist;
    }
}
