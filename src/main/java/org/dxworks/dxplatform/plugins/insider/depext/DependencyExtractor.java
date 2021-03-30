package org.dxworks.dxplatform.plugins.insider.depext;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DependencyExtractor {
    private final List<String> cppExtensions = Arrays.asList(".c", ".cpp", ".hpp", ".cc", ".h", ".hh", ".cxx", ".hxx");
    private final String javaExtension = "java";
    private final String kotlinExtension = "kt";
    private final String scalaExtension = "scala";
    private final String csExtension = "cs";
    private final String groovyExtension = "groovy";
    private final List<ImportResult> resultsList = new ArrayList<>();
    private int counterFiles = 0;
    private int countIgnoredFiles = 0;

    public void processAndWriteResults(List<InsiderFile> files) {
        files.forEach(this::execute);
        System.out.println("\n\nTotal files in folder: " + counterFiles);
        System.out.println("Files skipped: " + countIgnoredFiles + "\n");

        if (JavastackImportsProcessor.hasFiles()) System.out.println(JavastackImportsProcessor.statistics());
        if (CSharpImportsProcessor.counter > 0)
            System.out.println(CSharpImportsProcessor.counter + " C# files processed");
        if (CPPImportsProcessor.counter > 0) System.out.println(CPPImportsProcessor.counter + " C++ files processed");

        String outputFilename = "results/" + InsiderConfiguration.getInstance().getProjectID() + "-depext.csv";
        try {
            Path.of("results").toFile().mkdirs();
            PrintWriter outputFile = new PrintWriter(outputFilename);

            System.err.println("Writing " + resultsList.size() + " entries to output file: " + outputFilename);

            outputFile.println(ImportResult.header());
            resultsList.forEach(outputFile::println);
            outputFile.close();
        } catch (FileNotFoundException e) {
            System.err.println("\nCannot write to file: " + outputFilename);
        }
    }

    private void execute(InsiderFile insiderFile) {
        AbstractImportsProcessor processor = createProcessor(insiderFile);

        counterFiles++;
        if (counterFiles % 1000 == 0) System.out.print("*");

        if (processor != null) resultsList.add(processor.extract());
        else countIgnoredFiles++;
    }

    private AbstractImportsProcessor createProcessor(InsiderFile insiderFile) {
        String extension = extension(insiderFile.getPath());

        if (extension == null) return null;

        if (extension.endsWith(javaExtension)) return new JavastackImportsProcessor(insiderFile, "java");
        else if (extension.endsWith(kotlinExtension)) return new JavastackImportsProcessor(insiderFile, "kotlin");
        else if (extension.endsWith(scalaExtension)) return new JavastackImportsProcessor(insiderFile, "scala");
        else if (extension.endsWith(groovyExtension)) return new JavastackImportsProcessor(insiderFile, "groovy");
        else if (extension.endsWith(csExtension)) return new CSharpImportsProcessor(insiderFile);
        else if (cppExtensions.contains(extension)) return new CPPImportsProcessor(insiderFile);

        return null;
    }

    public boolean acceptFile(String path) {
        List<String> acceptedExtensions = new ArrayList<>(cppExtensions);
        acceptedExtensions.add(javaExtension);
        acceptedExtensions.add(kotlinExtension);
        acceptedExtensions.add(scalaExtension);
        acceptedExtensions.add(groovyExtension);
        acceptedExtensions.add(csExtension);
        return acceptedExtensions.stream().anyMatch(path::endsWith);
    }

    private String extension(String filename) {
        int extensionIndex = filename.lastIndexOf(".");
        return (extensionIndex == -1) ? null : filename.substring(extensionIndex).toLowerCase();
    }
}
