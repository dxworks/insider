package org.dxworks.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.InsiderResult;
import org.dxworks.insider.configuration.InsiderConfiguration;
import org.dxworks.insider.constants.InsiderConstants;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndentationCount implements AllFilesCommand {
    @Override
    public boolean parse(List<String> args) {
        return args.size() == 1;
    }

    @SneakyThrows
    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {
        List<InsiderResult> insiderResults = insiderFiles.stream().flatMap(file -> {
                    IntSummaryStatistics summary = Arrays.stream(file.getContent().split("\n"))
                            .filter(l -> !l.isBlank())
                            .mapToInt(this::getLineIndentation)
                            .summaryStatistics();

                    return Stream.of(new InsiderResult("max_indentation", "complexity", file.getFullyQualifiedName(), summary.getMax()),
                            new InsiderResult("avg_indentation", "complexity", file.getFullyQualifiedName(), (int) Math.round(summary.getAverage())),
                            new InsiderResult("total_indentation", "complexity", file.getFullyQualifiedName(), (int) summary.getSum())
                            );
                }
        ).collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.writeValue(Path.of(InsiderConstants.RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-indentation.json").toFile(), insiderResults);
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
            } else {
                break;
            }
        }
        return indentation / 2;
    }

    @Override
    public String usage() {
        return "insider indent";
    }

    @Override
    public String getName() {
        return INDENT;
    }
}
