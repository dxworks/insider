package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyPattern {
    private String pattern;
    private String type;
    private String confidence;
    private List<String> modifiers;
    private List<String> scopes;
    private String _comment;
}
