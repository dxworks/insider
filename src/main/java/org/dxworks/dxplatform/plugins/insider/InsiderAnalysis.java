package org.dxworks.dxplatform.plugins.insider;

import java.util.List;

public interface InsiderAnalysis {
    List<InsiderResult> analyze(InsiderFile insiderFile);

    boolean accepts(String extension);
}
