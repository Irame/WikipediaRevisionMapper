package de.mpii.wiki.result;

public class MappedResult {

  private final int sourceId;
  private final String sourceTitle;
  private final int targetId;
  private final String targetTitle;
  private final MappedType type;

  public MappedResult(int sourceId, String sourceTitle, int targetId, String targetTitle, MappedType type) {
    this.sourceId = sourceId;
    this.sourceTitle = sourceTitle;
    this.targetId = targetId;
    this.targetTitle = targetTitle;
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
}
