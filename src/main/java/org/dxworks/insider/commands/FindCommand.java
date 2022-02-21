package org.dxworks.insider.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.InsiderResult;
import org.dxworks.insider.configuration.InsiderConfiguration;
import org.dxworks.insider.technology.finder.model.FindCommandConfigFile;
import org.dxworks.insider.technology.finder.parsers.JsonFingerprintParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dxworks.insider.constants.InsiderConstants.RESULTS_FOLDER;

@Slf4j
public class FindCommand implements FilesCommand {

    private List<String> fingerprintFiles;
    private Map<String, List<InsiderResult>> resultsMap = new HashMap<>();
    private Map<String, FindCommandConfigFile> configurationMap = new HashMap<>();

    @Override
    public boolean parse(List<String> args) {
        if (args.size() == 1) {
            return false;
        }
        JsonFingerprintParser parser = new JsonFingerprintParser();

        List<String> files = args.subList(1, args.size());
        fingerprintFiles = files.stream().filter(this::fileExists).collect(Collectors.toList());
        fingerprintFiles.forEach(file -> {
            resultsMap.put(file, new ArrayList<>());
            configurationMap.put(file, parser.parseFile(file));

        });

        return !fingerprintFiles.isEmpty() && files.size() == fingerprintFiles.size();
    }

    @Override
    public String usage() {
        return "insider find <paths_to_json>...";
    }

    @Override
    public String getName() {
        return FIND;
    }

    @Override
    public void init(List<String> commandArgs) {

    }

    @Override
    public void analyse(InsiderFile insiderFile) {

        fingerprintFiles.parallelStream().forEach(file -> {
            FindCommandConfigFile configFile = configurationMap.get(file);
            if (configFile != null) {
                List<InsiderResult> insiderResults =
                        configFile.getTechnologies().parallelStream()
                                .map(technology -> technology.analyze(insiderFile))
                                .filter(Objects::nonNull)
                                .filter(insiderResult -> insiderResult.getValue() > 0).collect(Collectors.toList());

                resultsMap.get(file).addAll(insiderResults);
            }
        });
    }

    @Override
    public void writeResults() {
        fingerprintFiles.forEach(file -> {
            FindCommandConfigFile configFile = configurationMap.get(file);
            if (configFile != null) {
                String outputFileName = configFile.getOutputFile();
                if (outputFileName == null) {
                    outputFileName = InsiderConfiguration.getInstance().getProjectID() + "-" + Paths.get(file).toFile().getName();
                }

                ObjectMapper objectMapper = new ObjectMapper();
                File outputFile = Paths.get(RESULTS_FOLDER, outputFileName).toFile();
                try {
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileWriter(outputFile), resultsMap.get(file));
                } catch (IOException e) {
                    log.error("Could not write JSON file" + outputFile + "!", e);
                }
            }
        });
    }
}
