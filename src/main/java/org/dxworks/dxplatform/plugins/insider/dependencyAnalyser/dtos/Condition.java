package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Condition {
    private MyPattern pattern;
    private String search_in;
    private boolean negate_finding;
    private String _comment;
}
