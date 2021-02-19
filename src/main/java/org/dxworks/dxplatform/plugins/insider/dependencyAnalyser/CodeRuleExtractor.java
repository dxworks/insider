package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.PatternMatch;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Rule;

import java.util.List;

public class CodeRuleExtractor implements RuleExtractor {
    @Override
    public List<PatternMatch> extract(InsiderFile insiderFile, Rule rule) {
        return null;
    }
}
