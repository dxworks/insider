package org.dxworks.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dxworks.insider.ChronosTag;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.configuration.InsiderConfiguration;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dxworks.insider.constants.InsiderConstants.RESULTS_FOLDER;

public class MeasureCommand implements FilesCommand {

    private List<ChronosTag> chronosTags = new ArrayList<>();


    @Override
    public boolean parse(List<String> args) {
        return args.size() == 1;
    }

    @Override
    public String usage() {
        return "insider measure";
    }

    @Override
    public String getName() {
        return MEASURE;
    }

    @Override
    public void init(List<String> commandArgs) {

    }

    @Override
    public void analyse(InsiderFile file) {
        chronosTags.add(new ChronosTag(file.getFullyQualifiedName(), "loc", file.getLines()));
        chronosTags.add(new ChronosTag(file.getFullyQualifiedName(), "size", (int) file.getSize()));
    }

    @Override
    public void writeResults() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-size.json").toFile(), chronosTags);
        } catch (IOException e) {
            System.err.println("Measure command finished unsuccessfully");
            e.printStackTrace();
        }
    }
}
