package de.mpii.wiki.dump;

public enum DumpType {
    SOURCE, TARGET, SOURCE_EVAL, TARGET_EVAL;

    public boolean isSource() {
        return this == SOURCE || this == SOURCE_EVAL;
    }

    public boolean isTarget() {
        return this == TARGET || this == TARGET_EVAL;
    }

    public boolean isEval() {
        return this == SOURCE_EVAL || this == TARGET_EVAL;
    }
}
