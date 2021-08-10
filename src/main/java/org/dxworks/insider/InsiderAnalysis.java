package org.dxworks.insider;

public interface InsiderAnalysis {
    InsiderResult analyze(InsiderFile insiderFile);

    boolean accepts(InsiderFile insiderFile);
}
