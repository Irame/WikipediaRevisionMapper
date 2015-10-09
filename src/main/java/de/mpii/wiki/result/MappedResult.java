package de.mpii.wiki.result;

public class MappedResult {

    private final int sourceId;
    private final String sourceTitle;
    private final String sourceText;
    private final int targetId;
    private final String targetTitle;
    private final String targetText;
    private final MappedType type;

    public MappedResult(int sourceId, String sourceTitle, String sourceText, int targetId, String targetTitle, String targetText, MappedType type) {
        this.sourceId = sourceId;
        this.sourceTitle = sourceTitle;
        this.sourceText = sourceText;
        this.targetId = targetId;
        this.targetTitle = targetTitle;
        this.targetText = targetText;
        this.type = type;
    }

    public int getSourceId() {
        return sourceId;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public int getTargetId() {
        return targetId;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public MappedType getMappingType() {
        return type;
    }

    public String getSourceText() {
        return sourceText;
    }

    public String getTargetText() {
        return targetText;
    }
}
