package de.mpii.wiki.result;

public enum MappedType {
    DELETED("DELETED"), UNCHANGED("UNCHANGED"), UPDATED("UPDATED"),
    REDIRECTED("REDIRECTED"), DISAMBIGUATED("DISAMBIGUATED"),
    SOURCE_IGNORED("SOURCE_IGNORED"), REDIRECTED_CYCLE("REDIRECTED_CYCLE");

    String reprText;

    MappedType(String repr) {
        reprText = repr;
    }

    @Override
    public String toString() {
        return reprText;
    }
}
