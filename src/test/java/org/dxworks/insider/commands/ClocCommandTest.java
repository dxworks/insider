package org.dxworks.insider.commands;

import org.dxworks.utils.ignorer.Ignorer;
import org.dxworks.utils.ignorer.IgnorerBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClocCommandTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void symlinkToDirectoryIsSkippedAndDoesNotTruncateOutput() throws IOException {
        Path root = tmp.newFolder("root").toPath();
        Path target = tmp.newFolder("target").toPath();
        Files.writeString(target.resolve("inside.txt"), "x\n");

        // Names chosen so the link sorts between the two files in most directory orders;
        // the assertion below does not depend on order anyway.
        Files.writeString(root.resolve("a.txt"), "one\ntwo\nthree");
        Files.createSymbolicLink(root.resolve("m-link"), target);
        Files.writeString(root.resolve("z.txt"), "one\r\ntwo\r\n");

        Path csv = tmp.getRoot().toPath().resolve("out.csv");
        new ClocCommand().countFolder(root, csv, acceptAll());

        List<String> rows = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals("file,lines,size", rows.get(0));

        List<String> files = rows.stream().skip(1).map(r -> r.split(",")[0]).sorted().collect(Collectors.toList());
        assertEquals(List.of("\"a.txt\"", "\"z.txt\""), files);
        assertTrue(rows.contains("\"a.txt\",3,13"));
        assertTrue(rows.contains("\"z.txt\",3,10"));
        assertFalse(rows.stream().anyMatch(r -> r.contains("m-link")));
    }

    @Test
    public void symlinkToFileIsSkipped() throws IOException {
        Path root = tmp.newFolder("root").toPath();
        Files.writeString(root.resolve("real.txt"), "a\nb\n");
        Files.createSymbolicLink(root.resolve("alias.txt"), root.resolve("real.txt"));

        Path csv = tmp.getRoot().toPath().resolve("out.csv");
        new ClocCommand().countFolder(root, csv, acceptAll());

        List<String> rows = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(2, rows.size());
        assertEquals("\"real.txt\",3,4", rows.get(1));
    }

    @Test
    public void ignoredFilesAreNotCounted() throws IOException {
        Path root = tmp.newFolder("root").toPath();
        Files.writeString(root.resolve("keep.txt"), "a\n");
        Files.writeString(root.resolve("drop.log"), "a\n");

        Path csv = tmp.getRoot().toPath().resolve("out.csv");
        new ClocCommand().countFolder(root, csv, new IgnorerBuilder(List.of("**/*.log")).compile());

        List<String> rows = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(2, rows.size());
        assertEquals("\"keep.txt\",2,2", rows.get(1));
    }

    private static Ignorer acceptAll() {
        return new IgnorerBuilder(List.<String>of()).compile();
    }
}
