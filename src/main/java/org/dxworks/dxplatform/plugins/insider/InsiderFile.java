package org.dxworks.dxplatform.plugins.insider;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dxworks.dxplatform.plugins.insider.configuration.InsiderConfiguration;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
public class InsiderFile {

    private String name;
    private String path;
    private String extension;

    private String content;

    private List<Integer> lineBreaks;

    private int lines;

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
        return getPath().substring(InsiderConfiguration.getInstance().getRootFolder().length() + 1).replace('\\', '/');
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
        private List<Integer> lineBreaks;
        private int lines;

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

        public InsiderFileBuilder lineBreaks(List<Integer> lineBreaks) {
            this.lineBreaks = lineBreaks;
            return this;
        }

        public InsiderFileBuilder lines(int lines) {
            this.lines = lines;
            return this;
        }

        public InsiderFile build() {
            InsiderFile insiderFile = new InsiderFile(name, path, extension, content, lineBreaks, lines);
            insiderFile.init();
            return insiderFile;
        }

        public String toString() {
            return "InsiderFile.InsiderFileBuilder(name=" + this.name + ", path=" + this.path + ", extension=" + this.extension + ", content=" + this.content + ", lineBreaks=" + this.lineBreaks + ", lines=" + this.lines + ")";
        }
    }

    private void init() {
        extractLineBreaks(content);
        lines = lineBreaks.size();
    }
}
