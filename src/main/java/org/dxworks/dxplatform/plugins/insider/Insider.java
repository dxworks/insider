package org.dxworks.dxplatform.plugins.insider;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FilenameUtils;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;
import org.dxworks.dxplatform.plugins.insider.constants.InsiderConstants;
import org.dxworks.dxplatform.plugins.insider.library.detector.LibraryDetector;
import org.dxworks.dxplatform.plugins.insider.library.detector.LibraryDetectorLanguage;
import org.dxworks.dxplatform.plugins.insider.technology.finder.model.Technology;
import org.dxworks.dxplatform.plugins.insider.technology.finder.parsers.FingerprintsXmlParser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

//    @Option(names = {"-tf", "--technology-finder"},
//            description = {"TechnologyFinder is an analysis that find regular expressions",
//                    "( that should be provided in a configuration file )",
//                    "in your project files."},
//            paramLabel = "technology-finder")
//    private boolean technologyFinderFile;
//
//    @Option(names = {"-cif", "--clean", "--clean-imports-file"},
//            description = "The path to the imports CSV file you want to clean.",
//            paramLabel = "imports_file",
//            required = true)
//    private String cleanFile;
//
//    @Option(names = {"-diag", "--diag", "--diagnose-imports-file"},
//            description = "The path to the imports CSV file you want to diagnose.",
//            paramLabel = "imports_file",
//            required = true)
//    private String diagnoseFile;
//
//    @Option(names = {"-ld", "--library-discovery"},
//            description = {"Library discovery is a regex based analysis that finds all import statements",
//                    "in Java or C-based projects (C, Objective C, C++). It also aggregates the results",
//                    "on packages (for Java), or on libraries (through a header to library mapping file for C-based)"},
//            paramLabel = "library-discovery")
//    private boolean libraryDiscovery;


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

        List<InsiderFile> insiderFiles = new ArrayList<>();
        readProjectFiles(rootFolder, insiderFiles);

//        runTechnologyFinder(insiderFiles);

        LibraryDetector libraryDetector = new LibraryDetector(LibraryDetectorLanguage.JAVA);
        insiderFiles.stream()
                .filter(insiderFile -> libraryDetector.accepts(insiderFile.getExtension()))
                .forEach(libraryDetector::analyze);

        libraryDetector.generateResults();
    }

    private void readProjectFiles(String rootFolder, List<InsiderFile> insiderFiles) {
        try {
            List<Path> pathList = Files.walk(Paths.get(rootFolder)).collect(Collectors.toList());
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO: extract this to the Technology Finder analyzer.
    private void runTechnologyFinder(List<InsiderFile> insiderFiles) {

        FingerprintsXmlParser fingerprintsXmlParser = new FingerprintsXmlParser();

        List<Technology> technologies = fingerprintsXmlParser.parseTechnologiesFile("config\\libraries.xml");

        List<InsiderResult> insiderResults;

        try (ProgressBar pb = new ProgressBarBuilder()
                .setInitialMax(insiderFiles.size())
                .setUnit("Files", 1)
                .setTaskName("Matching")
                .setStyle(ProgressBarStyle.ASCII)
                .setUpdateIntervalMillis(100)
                .setPrintStream(System.err)
                .build()) {
            insiderResults = insiderFiles.parallelStream()
                    .flatMap(insiderFile -> {
                        Stream<InsiderResult> insiderResultStream = technologies.parallelStream()
                                .flatMap(technology -> technology.analyze(insiderFile).stream());
                        pb.step();
                        return insiderResultStream;
                    })
                    .collect(Collectors.toList());
        }
        int sum = insiderResults.stream().mapToInt(InsiderResult::getValue).sum();

        System.out.println(sum);
    }
}
