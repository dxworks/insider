package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Language;

import java.io.File;
import java.util.List;

public class LanguageService {

    @SneakyThrows
    public List<Language> getLanguages() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(getClass().getResource("/languages.json").getPath().substring(1)), new TypeReference<List<Language>>() {
        });
    }
}
