package org.dxworks.dxplatform.plugins.insider.application.inspector;

import org.dxworks.dxplatform.plugins.insider.InsiderFile;
import org.dxworks.dxplatform.plugins.insider.application.inspector.dtos.PatternMatch;
import org.dxworks.dxplatform.plugins.insider.application.inspector.dtos.Rule;

import java.util.List;

public class FileRuleExtractor implements RuleExtractor {
    @Override
    public List<PatternMatch> extract(InsiderFile insiderFile, Rule rule) {
        return null;
    }
}
