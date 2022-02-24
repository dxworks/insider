package org.dxworks.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dxworks.insider.ChronosTag;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.InsiderResult;
import org.dxworks.insider.configuration.InsiderConfiguration;
import org.dxworks.insider.constants.InsiderConstants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class IndentationCount implements FilesCommand {

    private List<InsiderResult> insiderResults = new ArrayList<>();

    @Override
    public boolean parse(List<String> args) {
        return args.size() == 1;
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

    @Override
    public void init(List<String> commandArgs) {

    }

    @Override
    public void analyse(InsiderFile file) {
        IntSummaryStatistics summary = Arrays.stream(file.getContent().split("\n"))
                .filter(l -> !l.isBlank())
                .mapToInt(this::getLineIndentation)
                .summaryStatistics();

        insiderResults.add(new InsiderResult("max_indentation", "complexity", file.getFullyQualifiedName(), summary.getMax()));
        insiderResults.add(new InsiderResult("avg_indentation", "complexity", file.getFullyQualifiedName(), (int) Math.round(summary.getAverage())));
        insiderResults.add(new InsiderResult("total_indentation", "complexity", file.getFullyQualifiedName(), (int) summary.getSum()));
    }

    @Override
    public void writeResults() {
        try {
            List<ChronosTag> chronosTags = insiderResults.stream().map(insiderResult -> new ChronosTag(insiderResult.getFile(), "complexity." + insiderResult.getName(), insiderResult.getValue())).collect(Collectors.toList());
            Map<String, Map<String, List<ChronosTag>>> chronosResult = Map.of("file", Map.of("concerns", chronosTags));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(Path.of(InsiderConstants.RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-indentation.json").toFile(), chronosResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
