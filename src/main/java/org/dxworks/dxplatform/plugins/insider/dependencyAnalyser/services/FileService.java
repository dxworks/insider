package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services;

import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.MyFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileService {

    @SneakyThrows
    public List<MyFile> getFilesFromFolder(String folder) {
        List<MyFile> files = new ArrayList<>();

        Files.walk(Paths.get(folder))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String extension = FilenameUtils.getExtension(path.getFileName().toString());
                        List<String> contentList = Files.readAllLines(path);
                        String content = contentList.stream()
                                .map(Object::toString)
                                .collect(Collectors.joining("\n"));
                        files.add(new MyFile(path.toString(), content, extension));
                    }
                    catch(Exception e) {
                        e.getStackTrace();
                    }
                });
        return files;
    }
}
