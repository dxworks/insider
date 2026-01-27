package org.dxworks.insider.depext;

import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.configuration.InsiderConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class RustImportsProcessorTest {

    private static final String DUMMY_CONTENT = "// test file\n";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File crateRoot;

    @Before
    public void setUp() throws Exception {
        // Reset cached rootFolder in singleton using reflection
        Field rootFolderField = InsiderConfiguration.class.getDeclaredField("rootFolder");
        rootFolderField.setAccessible(true);
        rootFolderField.set(InsiderConfiguration.getInstance(), null);
        
        // Now set the system property and load configuration
        System.setProperty("INSIDER_ROOT_FOLDER", tempFolder.getRoot().getAbsolutePath());
        System.setProperty("INSIDER_PROJECT_ID", "test-project");
        InsiderConfiguration.getInstance().load();
        
        crateRoot = createCrateWithName("test-crate");
    }

    @Test
    public void testLibRsNamespace() throws IOException {
        ImportResult result = processFile("src/lib.rs", DUMMY_CONTENT);
        assertEquals("test_crate", result.namespace);
        assertEquals("rust", result.language);
    }

    @Test
    public void testMainRsNamespace() throws IOException {
        ImportResult result = processFile("src/main.rs", DUMMY_CONTENT);
        assertEquals("test_crate", result.namespace);
    }

    @Test
    public void testModuleFileNamespace() throws IOException {
        ImportResult result = processFile("src/network.rs", DUMMY_CONTENT);
        assertEquals("test_crate::network", result.namespace);
    }

    @Test
    public void testNestedModuleNamespace() throws IOException {
        ImportResult result = processFile("src/network/tcp.rs", DUMMY_CONTENT);
        assertEquals("test_crate::network::tcp", result.namespace);
    }

    @Test
    public void testModRsNamespace() throws IOException {
        ImportResult result = processFile("src/network/mod.rs", DUMMY_CONTENT);
        assertEquals("test_crate::network", result.namespace);
    }

    @Test
    public void testCrateNameWithHyphens() throws IOException {
        File multiWordCrate = createCrateWithName("multi-word-crate");
        ImportResult result = processFileInCrate(multiWordCrate, "src/lib.rs", DUMMY_CONTENT);
        assertEquals("multi_word_crate", result.namespace);
    }

    @Test
    public void testSimpleUseStatement() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use std::collections::HashMap;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("std::collections::HashMap", result.importedItems.get(0).name);
        assertEquals("", result.importedItems.get(0).attribute);
    }

    @Test
    public void testMultipleImportsWithBraces() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use std::io::{Read, Write};\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("std::io::{Read, Write}", result.importedItems.get(0).name);
    }

    @Test
    public void testGlobImport() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use std::fs::*;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("std::fs::*", result.importedItems.get(0).name);
        assertEquals("glob", result.importedItems.get(0).attribute);
    }

    @Test
    public void testPubUseStatement() throws IOException {
        ImportResult result = processFile("src/lib.rs", "pub use tcp::Connection;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("tcp::Connection", result.importedItems.get(0).name);
        assertEquals("pub", result.importedItems.get(0).attribute);
    }

    @Test
    public void testCrateRelativeImport() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use crate::network::tcp;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("crate::network::tcp", result.importedItems.get(0).name);
    }

    @Test
    public void testSuperRelativeImport() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use super::utils;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("super::utils", result.importedItems.get(0).name);
    }

    @Test
    public void testFileNotUnderSrc() throws IOException {
        ImportResult result = processFile("benches/benchmark.rs", "use criterion::Criterion;\n");
        assertEquals("", result.namespace);
        assertEquals(1, result.importedItems.size());
        assertEquals("criterion::Criterion", result.importedItems.get(0).name);
    }

    @Test
    public void testMultiCrateWorkspace() throws IOException {
        File networkCrate = tempFolder.newFolder("workspace", "crates", "network-lib");
        File networkSrc = new File(networkCrate, "src");
        networkSrc.mkdirs();
        createCargoToml(networkCrate, "network-lib");

        ImportResult result = processFileInCrate(networkCrate, "src/session.rs", DUMMY_CONTENT);
        assertEquals("network_lib::session", result.namespace);
    }

    @Test
    public void testMultipleImportsInFile() throws IOException {
        String content = "use std::collections::HashMap;\n" +
                "use std::io::{Read, Write};\n" +
                "pub use tcp::Connection;\n" +
                "use std::fs::*;\n";
        ImportResult result = processFile("src/lib.rs", content);

        assertEquals(4, result.importedItems.size());
        assertEquals("std::collections::HashMap", result.importedItems.get(0).name);
        assertEquals("std::io::{Read, Write}", result.importedItems.get(1).name);
        assertEquals("tcp::Connection", result.importedItems.get(2).name);
        assertEquals("pub", result.importedItems.get(2).attribute);
        assertEquals("std::fs::*", result.importedItems.get(3).name);
        assertEquals("glob", result.importedItems.get(3).attribute);
    }

    @Test
    public void testLanguage() throws IOException {
        ImportResult result = processFile("src/lib.rs", "");
        assertEquals("rust", result.language);
    }

    @Test
    public void testPubUseWithMultipleSpaces() throws IOException {
        ImportResult result = processFile("src/lib.rs", "pub    use tcp::Connection;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("tcp::Connection", result.importedItems.get(0).name);
        assertEquals("pub", result.importedItems.get(0).attribute);
    }

    @Test
    public void testUseWithMultipleSpaces() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use    std::collections::HashMap;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("std::collections::HashMap", result.importedItems.get(0).name);
        assertEquals("", result.importedItems.get(0).attribute);
    }

    @Test
    public void testUseWithCommentBefore() throws IOException {
        ImportResult result = processFile("src/lib.rs", "/* some comment */ use std::io;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("std::io", result.importedItems.get(0).name);
    }

    @Test
    public void testPubUseWithCommentBefore() throws IOException {
        ImportResult result = processFile("src/lib.rs", "/* comment */ pub use tcp::Connection;\n");
        assertEquals(1, result.importedItems.size());
        assertEquals("tcp::Connection", result.importedItems.get(0).name);
        assertEquals("pub", result.importedItems.get(0).attribute);
    }

    @Test
    public void testCsvEscapingForImportsWithCommas() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use std::io::{Read, Write};\n");
        
        String csvOutput = result.toString();
        
        assertTrue("CSV output should contain quoted import with commas", 
                   csvOutput.contains("\"std::io::{Read, Write}\""));
        
        String[] fields = csvOutput.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        assertEquals("Should have 5 fields (file, lang, namespace, lines, import)", 5, fields.length);
        assertEquals("Import field should be properly quoted", "\"std::io::{Read, Write}\"", fields[4]);
    }

    private File createCrateWithName(String crateName) throws IOException {
        File crate = tempFolder.newFolder(crateName);
        File srcDir = new File(crate, "src");
        srcDir.mkdirs();
        createCargoToml(crate, crateName);
        return crate;
    }

    private void createCargoToml(File crateRoot, String crateName) throws IOException {
        String cargoToml = "[package]\n" +
                "name = \"" + crateName + "\"\n" +
                "version = \"0.1.0\"\n";
        Files.write(crateRoot.toPath().resolve("Cargo.toml"), cargoToml.getBytes());
    }

    private ImportResult processFile(String relativePath, String content) throws IOException {
        return processFileInCrate(crateRoot, relativePath, content);
    }

    private ImportResult processFileInCrate(File crate, String relativePath, String content) throws IOException {
        File file = new File(crate, relativePath);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content.getBytes());

        InsiderFile insiderFile = InsiderFile.builder()
                .name(file.getName())
                .path(file.getAbsolutePath())
                .extension(".rs")
                .content(content)
                .size(content.length())
                .build();

        RustImportsProcessor processor = new RustImportsProcessor(insiderFile);
        return processor.extract();
    }
}
