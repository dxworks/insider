package org.dxworks.insider.commands;

import lombok.extern.slf4j.Slf4j;
import org.dxworks.utils.ignorer.Ignorer;
import org.dxworks.utils.ignorer.IgnorerBuilder;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.configuration.InsiderConfiguration;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.dxworks.insider.constants.InsiderConstants.CONFIGURATION_FOLDER;
import static org.dxworks.insider.constants.InsiderConstants.RESULTS_FOLDER;

@Slf4j
public class ClocCommand implements NoFilesCommand {
    private static final int BUFFER_SIZE = 8192;

    @Override
    public boolean parse(List<String> args) {
        return args.size() == 1;
    }

    @Override
    public void execute(List<InsiderFile> insiderFiles, List<String> args) {
        processFolder(InsiderConfiguration.getInstance().getRootFolder());
    }

    @Override
    public String usage() {
        return "insider count";
    }

    @Override
    public String getName() {
        return COUNT;
    }


    public void processFolder(String folderPath) {
        Path startPath = Paths.get(folderPath);
        Path csvPath = Paths.get(RESULTS_FOLDER, InsiderConfiguration.getInstance().getProjectID() + "-cloc.csv");

        Ignorer ignorer = new IgnorerBuilder(Paths.get(CONFIGURATION_FOLDER, ".ignore")).compile();

        countFolder(startPath, csvPath, ignorer);
    }

    /**
     * Walks {@code startPath} and writes one CSV row per accepted regular file to {@code csvPath}.
     * <p>
     * Symbolic links, directories and other non-regular entries are skipped. A file that cannot be
     * read is logged and skipped, so a single bad entry never truncates the rest of the output.
     */
    void countFolder(Path startPath, Path csvPath, Ignorer ignorer) {
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            writer.write("file,lines,size\n");
            Files.walkFileTree(startPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile() || !ignorer.accepts(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        long lines = countLines(file);
                        long size = attrs.size();
                        String relativePath = startPath.relativize(file).toString();

                        writer.write("\"" + relativePath + "\"" + "," + lines + "," + size);
                        writer.newLine();
                    } catch (IOException e) {
                        log.warn("Could not count lines in {}: {}", file, e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    log.warn("Could not visit {}: {}", file, e.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Could not write line counts to {}", csvPath, e);
        }
    }

    private long countLines(Path path) throws IOException {
        long lines = 0;
        try (InputStream in = Files.newInputStream(path);
             BufferedInputStream bis = new BufferedInputStream(in)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while ((n = bis.read(buffer)) != -1) {
                for (int i = 0; i < n; i++) {
                    if (buffer[i] == '\n' || buffer[i] == '\r') {
                        lines++;
                        // Handle CRLF endings
                        if (i < n - 1 && buffer[i] == '\r' && buffer[i + 1] == '\n') {
                            i++;
                        }
                    }
                }
            }
        }
        return lines + 1;
    }
}
