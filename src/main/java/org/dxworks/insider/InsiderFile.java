package org.dxworks.insider;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dxworks.insider.configuration.InsiderConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Data
public class InsiderFile {

    private final String name;
    private final String path;
    private final String extension;

    private final String content;

    private List<Integer> lineBreaks;

    private int lines;
    private final long size;

    private String fullyQualifiedName;

    public static InsiderFileBuilder builder() {
        return new InsiderFileBuilder();
    }

    public String getLine(Integer index) {
        if (index == lines - 1)
            return (content.substring(lineBreaks.get(index) + 1));
        return content.substring(lineBreaks.get(index) + 1, lineBreaks.get(index + 1) + 1);
    }

    private void extractLineBreaks(String content) {
        lineBreaks = new ArrayList<>();
        lineBreaks.add(-1);
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lineBreaks.add(i);
            }
        }
    }

    public String getFullyQualifiedName() {
        if (fullyQualifiedName == null)
            fullyQualifiedName = Path.of(InsiderConfiguration.getInstance().getRootFolder()).toAbsolutePath()
                    .relativize(Path.of(getPath()).toAbsolutePath())
                    .normalize().toString().replace("\\", "/");
        return fullyQualifiedName;
    }

    public int getLineNumberOfAbsoluteCharacterIndex(int index) {
        for (int i = 0; i < lineBreaks.size(); i++) {
            if (index <= lineBreaks.get(i))
                return i;
        }
        return lineBreaks.size();
    }

    public int getLineIndexFromFileIndex(int fileIndex) {
        return fileIndex - lineBreaks.get(getLineNumberOfAbsoluteCharacterIndex(fileIndex) - 1);
    }

    public static class InsiderFileBuilder {
        private String name;
        private String path;
        private String extension;
        private String content;
        private long size;

        InsiderFileBuilder() {
        }

        public InsiderFileBuilder name(String name) {
            this.name = name;
            return this;
        }

        public InsiderFileBuilder path(String path) {
            this.path = path;
            return this;
        }

        public InsiderFileBuilder extension(String extension) {
            this.extension = extension;
            return this;
        }

        public InsiderFileBuilder content(String content) {
            this.content = content;
            return this;
        }

        public InsiderFileBuilder size(long size) {
            this.size = size;
            return this;
        }

        public InsiderFile build() {
            InsiderFile insiderFile = new InsiderFile(name, path, extension, content, size);
            insiderFile.init();
            return insiderFile;
        }

        @Override
        public String toString() {
            return "InsiderFileBuilder{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", extension='" + extension + '\'' +
                ", content='" + content + '\'' +
                ", size=" + size +
                '}';
        }
    }

    private void init() {
        extractLineBreaks(content);
        lines = lineBreaks.size();
    }
}
