package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Rule;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RuleService {

    @SneakyThrows
    public List<Rule> getRulesFromFile(String baseFolder) {
        List<Rule> rules = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();

        Files.walk(Paths.get(baseFolder))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("json"))
                .forEach(path -> {
                    try {
                        List<Rule> fileRules = objectMapper.readValue(path.toFile(), new TypeReference<List<Rule>>() {
                        });

                        rules.addAll(fileRules);
                    } catch (Exception e) {
                        e.getStackTrace();
                    }
                });

        return rules;
    }
}
