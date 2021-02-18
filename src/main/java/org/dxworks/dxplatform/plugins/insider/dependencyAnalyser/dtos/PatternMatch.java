package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PatternMatch {
    private String matchContent;
    private int startLine;
    private int endLine;
    private int globalStartIndex;
    private int globalEndIndex;
    private int startIndex;
    private int endIndex;
}
