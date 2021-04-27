package org.dxworks.dxplatform.plugins.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.InsiderResult;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants.RESULTS_FOLDER;

public class IndentationCount implements InsiderCommand {
    @Override
    public boolean parse(String[] args) {
        return args.length == 1;
    }

    @SneakyThrows
    @Override
    public void execute(List<InsiderFile> insiderFiles, String[] args) {
        List<InsiderResult> insiderResults = insiderFiles.stream().flatMap(file -> {
                    IntSummaryStatistics summary = Arrays.stream(file.getContent().split("\n"))
                            .filter(l -> !l.isBlank())
                            .mapToInt(this::getLineIndentation)
                            .summaryStatistics();

                    return Stream.of(new InsiderResult("max_indentation", "complexity", file.getFullyQualifiedName(), summary.getMax()),
                            new InsiderResult("avg_indentation", "complexity", file.getFullyQualifiedName(), (int) summary.getAverage()),
                            new InsiderResult("total_indentation", "complexity", file.getFullyQualifiedName(), (int) summary.getSum())
                            );
                }
        ).collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.writeValue(Path.of(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-indentation.json").toFile(), insiderResults);
    }

    private int getLineIndentation(String l) {
        int indentation = 0;
        for (int i = 0; i < l.toCharArray().length; i++) {
            char ch = l.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (ch == '\t')
                    indentation += 2;
                if (ch == ' ')
                    indentation += 1;
            }
        }
        return indentation / 2;
    }

    @Override
    public String usage() {
        return "insider indent";
    }
}