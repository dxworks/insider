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
    public void testPathAttributeInLibRsForFileDirectlyUnderSrc() throws IOException {
        processFile("src/lib.rs", "#[path = \"network.rs\"]\nmod net;\n");
        ImportResult result = processFile("src/network.rs", DUMMY_CONTENT);
        assertEquals("test_crate::net", result.namespace);
    }

    @Test
    public void testPathAttributeInSiblingModuleFileForNestedFile() throws IOException {
        // Simulate src/ui/select_dropdown.rs declaring a nested module file via #[path]
        processFile("src/ui/select_dropdown.rs", "#[path = \"select_dropdown/choice.rs\"]\nmod choice;\n");
        ImportResult result = processFile("src/ui/select_dropdown/choice.rs", DUMMY_CONTENT);
        assertEquals("test_crate::ui::select_dropdown::choice", result.namespace);
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
    public void testCsvEscapingForImportsWithCommas() throws IOException {
        ImportResult result = processFile("src/lib.rs", "use std::io::{Read, Write};\n");
        
        String csvOutput = result.toString();
        
        assertTrue("CSV output should contain quoted import with commas", 
                   csvOutput.contains("\"std::io::{Read, Write}\""));
        
        String[] fields = csvOutput.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        assertEquals("Should have 5 fields (file, lang, namespace, lines, import)", 5, fields.length);
        assertEquals("Import field should be properly quoted", "\"std::io::{Read, Write}\"", fields[4]);
    }

    @Test
    public void testMultiLineUseStatement() throws IOException {
        String content = "use security::{\n" +
                "    hash::Hash,\n" +
                "    crypto::Encrypt,\n" +
                "};\n";
        ImportResult result = processFile("src/lib.rs", content);
        
        assertEquals(1, result.importedItems.size());
        assertEquals("security::{ hash::Hash, crypto::Encrypt, }", result.importedItems.get(0).name);
        assertEquals("", result.importedItems.get(0).attribute);
    }

    @Test
    public void testMultiLinePubUseStatement() throws IOException {
        String content = "pub use security::{\n" +
                "    hash::Hash,\n" +
                "    crypto::Encrypt,\n" +
                "};\n";
        ImportResult result = processFile("src/lib.rs", content);
        
        assertEquals(1, result.importedItems.size());
        assertEquals("security::{ hash::Hash, crypto::Encrypt, }", result.importedItems.get(0).name);
        assertEquals("pub", result.importedItems.get(0).attribute);
    }

    @Test
    public void testMultiLineUseWithNestedBraces() throws IOException {
        String content = "use std::{\n" +
                "    io::{Read, Write},\n" +
                "    fs::File,\n" +
                "};\n";
        ImportResult result = processFile("src/lib.rs", content);
        
        assertEquals(1, result.importedItems.size());
        assertEquals("std::{ io::{Read, Write}, fs::File, }", result.importedItems.get(0).name);
    }

    @Test
    public void testMultipleMultiLineUseStatements() throws IOException {
        String content = "use security::{\n" +
                "    hash::Hash,\n" +
                "};\n" +
                "use crypto::{\n" +
                "    encrypt::Encrypt,\n" +
                "};\n";
        ImportResult result = processFile("src/lib.rs", content);
        
        assertEquals(2, result.importedItems.size());
        assertEquals("security::{ hash::Hash, }", result.importedItems.get(0).name);
        assertEquals("crypto::{ encrypt::Encrypt, }", result.importedItems.get(1).name);
    }

    @Test
    public void testMixedSingleAndMultiLineUseStatements() throws IOException {
        String content = "use std::collections::HashMap;\n" +
                "use security::{\n" +
                "    hash::Hash,\n" +
                "    crypto::Encrypt,\n" +
                "};\n" +
                "pub use tcp::Connection;\n";
        ImportResult result = processFile("src/lib.rs", content);
        
        assertEquals(3, result.importedItems.size());
        assertEquals("std::collections::HashMap", result.importedItems.get(0).name);
        assertEquals("security::{ hash::Hash, crypto::Encrypt, }", result.importedItems.get(1).name);
        assertEquals("tcp::Connection", result.importedItems.get(2).name);
        assertEquals("pub", result.importedItems.get(2).attribute);
    }

    @Test
    public void testMultiLineUseWithGlobImport() throws IOException {
        String content = "use security::{\n" +
                "    hash::*,\n" +
                "    crypto::Encrypt,\n" +
                "};\n";
        ImportResult result = processFile("src/lib.rs", content);
        
        assertEquals(1, result.importedItems.size());
        assertEquals("security::{ hash::*, crypto::Encrypt, }", result.importedItems.get(0).name);
        assertEquals("glob", result.importedItems.get(0).attribute);
    }

    @Test
    public void testMultiLineUseWithComments() throws IOException {
        String content = "use security::{\n" +
                "    // Hash algorithm\n" +
                "    hash::Hash,\n" +
                "    /* Encryption */ crypto::Encrypt,\n" +
                "};\n";
        ImportResult result = processFile("src/lib.rs", content);
        
        // Should successfully parse multi-line use statement even with comments
        assertEquals(1, result.importedItems.size());
        // The exact format after comment removal may vary, but it should be a single joined line
        assertNotNull(result.importedItems.get(0).name);
        assertFalse("Import name should not be empty", result.importedItems.get(0).name.isEmpty());
    }

    @Test
    public void testPathAttributeInParentModule() throws IOException {
        // Create parent module with #[path] attribute
        String parentModContent = "#[path = \"custom/module/mod.rs\"]\n" +
                "pub mod module;\n";
        processFile("src/network/mod.rs", parentModContent);
        
        // Create the actual file at the physical location
        String moduleContent = "pub struct Connection {}\n";
        ImportResult result = processFile("src/network/custom/module/mod.rs", moduleContent);
        
        // Should be network::module (logical path) not network::custom::module (physical path)
        assertEquals("test_crate::network::module", result.namespace);
    }

    @Test
    public void testMultiLineUseWithCRLF() throws IOException {
        // Test with CRLF line endings and multi-line use statements
        String content = "//! Module documentation\r\n" +
                "\r\n" +
                "use std::sync::OnceLock;\r\n" +
                "\r\n" +
                "use thiserror::Error;\r\n" +
                "\r\n" +
                "use super::http::{Request,\r\n" +
                "  Response,\r\n" +
                "  Config\r\n" +
                "};\r\n" +
                "\r\n" +
                "pub fn make_request() {}\r\n";
        
        ImportResult result = processFile("src/network/api_client.rs", content);
        
        assertEquals("test_crate::network::api_client", result.namespace);
        assertEquals(3, result.importedItems.size());
        assertEquals("std::sync::OnceLock", result.importedItems.get(0).name);
        assertEquals("thiserror::Error", result.importedItems.get(1).name);
        assertEquals("super::http::{Request, Response, Config }", result.importedItems.get(2).name);
    }

    @Test
    public void testComplexMultiLineUseWithManyItems() throws IOException {
        // Test a real-world complex multi-line use statement with many items
        String content = "use myapp::{\n" +
                "  error::{Error, Stage, ResourceError},\n" +
                "  DiagnosticsLevel, App, AppBuilder, FetchedResource, OutputFormat,\n" +
                "  Diagnostics, Options, Fetcher, ResourceKind,\n" +
                "};\n" +
                "\n" +
                "fn main() {}\n";
        
        ImportResult result = processFile("src/main.rs", content);
        
        assertEquals("test_crate", result.namespace);
        assertEquals(1, result.importedItems.size());
        assertEquals("myapp::{ error::{Error, Stage, ResourceError}, DiagnosticsLevel, App, AppBuilder, FetchedResource, OutputFormat, Diagnostics, Options, Fetcher, ResourceKind, }", 
                     result.importedItems.get(0).name);
    }

    @Test
    public void testStringLiteralWithSlashesAndSubsequentCode() throws IOException {
        // Test that code after a string literal with // is not incorrectly captured as imports
        // This tests the case where a multi-line string literal contains "use" and "//"
        String content = "use std::sync::Arc;\n" +
                "\n" +
                "#[cfg(not(feature = \"network\"))]\n" +
                "{\n" +
                "  Err(Error::Other(\n" +
                "    \"no Fetcher provided and `network` feature is disabled; \\\n" +
                "use AppBuilder::with_fetcher(...) (or App::with_config_and_fetcher) to inject one\"\n" +
                "      .to_string(),\n" +
                "  ))\n" +
                "}\n" +
                "\n" +
                "fn build_cache(\n" +
                "  base_url: &Option<String>,\n" +
                "  fetcher: Arc<dyn Fetcher>,\n" +
                ") -> Cache {\n" +
                "  match base_url {\n" +
                "    Some(url) => Cache::with_base_url(url.clone()),\n" +
                "    None => Cache::new(),\n" +
                "  }\n" +
                "}\n";
        
        ImportResult result = processFile("src/api.rs", content);
        
        // Should only extract the actual use statement at the top
        assertEquals(1, result.importedItems.size());
        assertEquals("std::sync::Arc", result.importedItems.get(0).name);
    }

    @Test
    public void testRelativePathWithoutLeadingSlash() throws IOException {
        // This test catches the bug where relative paths like "src/ui/module.rs"
        // (without leading slash) were not recognized as being under src/
        File file = new File(crateRoot, "src/ui/module.rs");
        file.getParentFile().mkdirs();
        String content = "use std::collections::HashMap;\n";
        Files.write(file.toPath(), content.getBytes());

        // Override getFullyQualifiedName to return path without leading slash
        InsiderFile testFile = new InsiderFile(
                file.getName(),
                file.getAbsolutePath(),
                ".rs",
                content,
                content.length()
        ) {
            @Override
            public String getFullyQualifiedName() {
                return "src/ui/module.rs"; // No leading slash
            }
        };

        RustImportsProcessor processor = new RustImportsProcessor(testFile);
        ImportResult result = processor.extract();

        // Should calculate namespace correctly even without leading slash
        assertEquals("test_crate::ui::module", result.namespace);
        assertEquals(1, result.importedItems.size());
        assertEquals("std::collections::HashMap", result.importedItems.get(0).name);
    }

    @Test
    public void testRawStringLiteralWithSlashesAndUseTextDoesNotBreakImportExtraction() throws IOException {
        String content = "use std::sync::Arc;\n" +
                "\n" +
                "let s = r#\"this looks like a comment // and also mentions use std::fs::*;\"#;\n" +
                "\n" +
                "fn main() {}\n";

        ImportResult result = processFile("src/raw.rs", content);

        assertEquals(1, result.importedItems.size());
        assertEquals("std::sync::Arc", result.importedItems.get(0).name);
    }

    @Test
    public void testCaseInsensitiveFileNames() throws IOException {
        // Test that file name detection (lib.rs, main.rs, mod.rs) is case-insensitive
        File srcDir = new File(crateRoot, "src");
        srcDir.mkdirs();
        
        // Test uppercase LIB.RS
        File libFile = new File(srcDir, "LIB.RS");
        String content = "use std::collections::HashMap;\n";
        Files.write(libFile.toPath(), content.getBytes());

        InsiderFile testFile = new InsiderFile(
                libFile.getName(),
                libFile.getAbsolutePath(),
                ".rs",
                content,
                content.length()
        ) {
            @Override
            public String getFullyQualifiedName() {
                return "src/LIB.RS";
            }
        };

        RustImportsProcessor processor = new RustImportsProcessor(testFile);
        ImportResult result = processor.extract();

        // Should recognize LIB.RS as crate root
        assertEquals("test_crate", result.namespace);
        assertEquals(1, result.importedItems.size());
        assertEquals("std::collections::HashMap", result.importedItems.get(0).name);
        
        // Test uppercase MOD.RS
        File networkDir = new File(srcDir, "network");
        networkDir.mkdirs();
        File modFile = new File(networkDir, "MOD.RS");
        content = "use std::io::Read;\n";
        Files.write(modFile.toPath(), content.getBytes());

        InsiderFile testFile2 = new InsiderFile(
                modFile.getName(),
                modFile.getAbsolutePath(),
                ".rs",
                content,
                content.length()
        ) {
            @Override
            public String getFullyQualifiedName() {
                return "src/network/MOD.RS";
            }
        };

        RustImportsProcessor processor2 = new RustImportsProcessor(testFile2);
        ImportResult result2 = processor2.extract();

        // Should recognize MOD.RS as representing the network module
        assertEquals("test_crate::network", result2.namespace);
        assertEquals(1, result2.importedItems.size());
        assertEquals("std::io::Read", result2.importedItems.get(0).name);
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
