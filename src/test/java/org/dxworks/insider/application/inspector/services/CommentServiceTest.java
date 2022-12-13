package org.dxworks.insider.application.inspector.services;

import org.apache.commons.lang.math.IntRange;
import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.technology.finder.LinguistService;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.dxworks.insider.constants.InsiderConstants.DEFAULT_LINGUIST_FILE;
import static org.junit.Assert.assertEquals;

public class CommentServiceTest {

    @Test
    public void extractCommentsForCobol() throws IOException {

        LinguistService.getInstance().initLinguist(DEFAULT_LINGUIST_FILE);

        String filePathAsString = getClass().getClassLoader().getResource("cobol/CM201M.CBL").getPath();
        File file = new File(filePathAsString);
        String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

        CommentService commentService = CommentService.getInstance();
        List<IntRange> commentRanges = commentService.extractInlineCommentLines(
                InsiderFile.builder()
                        .name("CM201M.CBL")
                        .path("cobol/CM201M.CBL")
                        .extension(".CBL")
                        .content(content)
                        .build());

        assertEquals(6, commentRanges.size());

    }
}