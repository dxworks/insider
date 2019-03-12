package org.dxworks.dxplatform.plugins.insider;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FilenameUtils;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;
import org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants;
import org.dxworks.dxplatform.plugins.insider.technology.finder.parsers.FingerprintsXmlParser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.xml.bind.JAXBException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Command(version = "Insider v0.0.1", header = "%nInsider%n",
        description = {"Insider is a semantic analysis tool.",
                "It searches for 'fingerprints' in your project and helps",
                "finding how much your project relies on libraries."})
public class Insider implements Runnable {


    @Option(names = {"-f", "--file"},
            description = "The path to the configuration file.",
            paramLabel = "configuration_file",
            required = true)
    private String configFile;

    @Option(names = {"-h", "--help"}, usageHelp = true,
            description = "Print usage help and exit.")
    private boolean usageHelpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true,
            description = "Print version information and exit.")
    private boolean versionRequested;

    @Option(names = {"-tf", "--technology-finder"},
            description = {"TechnologyFinder is an analysis that find regular expressions",
                    "( that should be provided in a configuration file )",
                    "in your project files."},
            paramLabel = "technology-finder")
    private boolean technologyFinderFile;

    @Option(names = {"-ld", "--library-discovery"},
            description = {"Library discovery is a regex based analysis that finds all import statements",
                    "in Java or C-based projects (C, Objective C, C++). It also aggregates the results",
                    "on packages (for Java), or on libraries (through a header to library mapping file for C-based)"},
            paramLabel = "library-discovery")
    private boolean libraryDiscovery;

    public static void main(String[] args) {
        CommandLine.run(new Insider(), System.out, args);
    }

    @Override
    public void run() {
        System.out.println("Reading configuration file: " + configFile);

        Path configFilePath = Paths.get(configFile);
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(configFilePath.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        InsiderConfiguration.loadProperties(properties);

        String rootFolder = InsiderConfiguration.getInstance().getProperty(InsiderConstants.ROOT_FOLDER);

        try {
            List<Path> pathList = Files.walk(Paths.get(rootFolder)).collect(Collectors.toList());
            List<InsiderFile> insiderFiles = new ArrayList<>();
            try (ProgressBar pb = new ProgressBar("Reading files", pathList.size(), ProgressBarStyle.ASCII)) {
                for (Path path : pathList) {
                    pb.step();
                    if (FilenameUtils.getExtension(path.getFileName().toString()).equals("java")) {
                        try {
                            insiderFiles.add(InsiderFile.builder()
                                    .path(path.toAbsolutePath().toString())
                                    .name(path.getFileName().toString())
                                    .extension(FilenameUtils.getExtension(path.getFileName().toString()))
                                    .content(new String(Files.readAllBytes(path)))
                                    .build());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            FingerprintsXmlParser fingerprintsXmlParser = new FingerprintsXmlParser();

            List<String> fingerPrints = fingerprintsXmlParser.parseString(Paths.get("C:\\Users\\Mario\\Desktop\\libraries.xml").toFile());

            List<Pattern> patternStream = fingerPrints.stream().map(Pattern::compile).collect(Collectors.toList());

            int result = 0;
            try (ProgressBar pb = new ProgressBarBuilder()
                    .setInitialMax(insiderFiles.size())
                    .setUnit("Files", 1)
                    .setTaskName("Matching fingerprints")
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUpdateIntervalMillis(100)
                    .setPrintStream(System.err)
                    .build()) {
                pb.maxHint(insiderFiles.size());
                for (InsiderFile insiderFile : insiderFiles) {
                    pb.step();
                    result += patternStream.stream().mapToInt(pattern -> {
                        int fileOcc = 0;
                        Matcher matcher = pattern.matcher(insiderFile.getContent());

                        while (matcher.find())
                            fileOcc++;

                        return fileOcc;
                    }).sum();
                }
            }

            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }
}
