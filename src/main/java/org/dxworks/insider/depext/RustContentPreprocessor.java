package org.dxworks.insider.depext;

import org.dxworks.insider.InsiderFile;

/**
 * Preprocesses Rust source content before import extraction.
 * 
 * <h2>Why preprocessing is needed</h2>
 * <pre>
 * Problem 1: String literals can contain "use" or "//" which would confuse parsing.
 *   Example: let msg = "use std::fs::*; // comment";
 *   Solution: Replace string contents with spaces, keeping delimiters.
 * 
 * Problem 2: Use statements can span multiple lines.
 *   Example: use std::{
 *              io::Read,
 *              fs::File,
 *            };
 *   Solution: Join into single line before line-by-line parsing.
 * </pre>
 */
public class RustContentPreprocessor {

    public InsiderFile preprocess(InsiderFile file) {
        String content = file.getContent();
        content = neutralizeStringLiterals(content);
        content = joinMultiLineUseStatements(content);
        
        return InsiderFile.builder()
            .name(file.getName())
            .path(file.getPath())
            .extension(file.getExtension())
            .content(content)
            .size(file.getSize())
            .build();
    }

    /**
     * Replace string literal contents with spaces.
     * 
     * Input:  let x = "use foo; // bar";
     * Output: let x = "                ";
     * 
     * Preserves: quotes (for structure), newlines (for line counting)
     */
    String neutralizeStringLiterals(String content) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        
        while (i < content.length()) {
            char c = content.charAt(i);
            
            if (c == '"') {
                out.append('"');
                i++;
                // Process string contents until closing quote
                while (i < content.length()) {
                    char ch = content.charAt(i);
                    
                    if (ch == '\\' && i + 1 < content.length()) {
                        // Escape sequence: skip both chars
                        out.append("  ");
                        i += 2;
                    } else if (ch == '"') {
                        // End of string
                        out.append('"');
                        i++;
                        break;
                    } else if (ch == '\n' || ch == '\r') {
                        // Keep newlines for line counting
                        out.append(ch);
                        i++;
                    } else {
                        // Replace content with space
                        out.append(' ');
                        i++;
                    }
                }
            } else {
                out.append(c);
                i++;
            }
        }
        
        return out.toString();
    }

    /**
     * Join multi-line use statements into single lines.
     * 
     * A use statement is complete when:
     * - It ends with semicolon AND
     * - All braces are balanced (braceDepth == 0)
     */
    String joinMultiLineUseStatements(String content) {
        StringBuilder result = new StringBuilder();
        StringBuilder useBuffer = new StringBuilder();
        boolean inUse = false;
        int braceDepth = 0;
        
        String[] lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (!inUse && isUseStart(trimmed)) {
                // Start of a use statement
                inUse = true;
                useBuffer.setLength(0);
                useBuffer.append(line);
                braceDepth = count(line, '{') - count(line, '}');
            } else if (inUse) {
                // Continue accumulating use statement
                useBuffer.append(" ").append(trimmed);
                braceDepth += count(line, '{') - count(line, '}');
            } else {
                // Regular line
                result.append(line).append("\n");
                continue;
            }
            
            // Check if use statement is complete
            if (inUse && trimmed.endsWith(";") && braceDepth == 0) {
                result.append(useBuffer).append("\n");
                inUse = false;
            }
        }
        
        // Handle unclosed use statement at end of file
        if (inUse) {
            result.append(useBuffer).append("\n");
        }
        
        return result.toString();
    }

    private boolean isUseStart(String line) {
        return line.matches("^(pub\\s+)?use\\s+.*");
    }
    
    private int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) n++;
        }
        return n;
    }
}
