package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    private String name;
    private String id;
    private String description;
    private List<String> applies_to;
    private List<String> tags;
    private String severity;
    private List<MyPattern> patterns;
    private List<String> overrides;
    private List<Condition> conditions;
    private String _comment;
}
