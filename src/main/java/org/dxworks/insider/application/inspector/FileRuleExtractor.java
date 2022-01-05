package org.dxworks.insider.application.inspector;

import org.dxworks.insider.InsiderFile;
import org.dxworks.insider.application.inspector.dtos.PatternMatch;
import org.dxworks.insider.application.inspector.dtos.Rule;

import java.util.List;

public class FileRuleExtractor implements RuleExtractor {
    @Override
    public List<PatternMatch> extract(InsiderFile insiderFile, Rule rule) {
        return null;
    }
}
