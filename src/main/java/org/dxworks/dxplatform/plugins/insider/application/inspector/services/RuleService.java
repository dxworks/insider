package org.dxworks.dxplatform.plugins.insider.application.inspector.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.dxplatform.plugins.insider.application.inspector.dtos.Rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RuleService {

    public List<Rule> getRuleFromFiles(List<String> ruleFiles) {

        List<Path> ruleFilePaths = ruleFiles.stream()
                .map(Paths::get)
                .flatMap(fileOrFolder -> {
                    if (Files.isDirectory(fileOrFolder)) {
                        return getAllRuleFilesFromFolder(fileOrFolder);
                    } else return Stream.of(fileOrFolder);
                }).collect(Collectors.toList());

        return ruleFilePaths.stream()
                .flatMap(ruleFile -> getRulesFromFile(ruleFile).stream())
                .collect(Collectors.toList());
    }

    private Stream<Path> getAllRuleFilesFromFolder(Path fileOrFolder) {
        try {
            return Files.walk(fileOrFolder).filter(path -> path.getFileName().toString().endsWith(".json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    private List<Rule> getRulesFromFile(Path ruleFilePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(ruleFilePath.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            log.warn("Could not read rule file!", e);
        }
        return new ArrayList<>();
    }
}
