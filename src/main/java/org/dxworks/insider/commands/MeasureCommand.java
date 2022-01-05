package org.dxworks.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dxworks.insider.ChronosTag;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.configuration.InsiderConfiguration;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dxworks.insider.constants.InsiderConstants.RESULTS_FOLDER;

public class MeasureCommand implements InsiderCommand {
    @Override
    public boolean parse(List<String> args) {
        return args.size() == 1;
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {
        List<ChronosTag> sizeTags = insiderFiles.stream().flatMap(insiderFile -> Stream.of(
            new ChronosTag(insiderFile.getFullyQualifiedName(), "loc", insiderFile.getLines()),
            new ChronosTag(insiderFile.getFullyQualifiedName(), "size", (int) insiderFile.getSize()))
        ).collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-size.json").toFile(), sizeTags);
        } catch (IOException e) {
            System.err.println("Measure command finished unsuccessfully");
            e.printStackTrace();
        }
    }

    @Override
    public String usage() {
        return "insider measure";
    }

    @Override
    public String getName() {
        return MEASURE;
    }
}
