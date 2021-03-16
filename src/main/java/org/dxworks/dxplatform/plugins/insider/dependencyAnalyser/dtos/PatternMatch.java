package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang.math.IntRange;

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

    public IntRange getAbsoluteRange() {
        return new IntRange(globalEndIndex, globalEndIndex);
    }
}
