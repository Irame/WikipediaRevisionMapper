package de.mpii.wiki.dump;

public enum PageType {
    ARTICLE, REDIRECT, DISAMBIGUATION;

    private static final String[] DISAMBIGUATION_TERMS = new String[] {
            "{{Disambig}}","{{Airport_disambig}}","{{Battledist}}",
            "{{Callsigndis}}","{{Chemistry disambiguation}}",
            "{{Church_disambig}}","{{Disambig-Chinese-char-title}}",
            "{{Disambig-cleanup}}","{{Genus_disambiguation}}",
            "{{Geodis}}","{{Hndis}}","{{Hndis-cleanup}}","{{Hospitaldis}}",
            "{{Hurricane_disambig}}","{{Letter_disambig}}",
            "{{Letter-NumberCombDisambig}}","{{Mathdab}}","{{MolFormDisambig}}",
            "{{NA_Broadcast_List}}","{{Numberdis}}","{{Schooldis}}",
            "{{Species_Latin name abbreviation disambiguation}}",
            "{{Taxonomy_disambiguation}}","{{Species_Latin_name_disambiguation}}",
            "{{WP_disambig}}", "{{Given name}}", "{{Surname}}",
            // old entries
            "{{Dab}}","{{Disambiguation}}","{{Geodab}}","{{Geo-dis}}",
            "{{Disambig|geo}}", "{{Disambig-CU}}", "{{Disamb}}",
            "Category:Disambiguation pages",
            "Category:Molecular formula disambiguation pages"
    };

    private static final String[] TITLE_DISAMBIGUATION_TERMS = new String[] {
        "(disambiguation)", "(Surname)", "(Given name)", "(Name)"
    };


    private static final String[] REDIRECT_TERMS = new String[] {
            "#REDIRECT"
    };

    public static PageType parseFrom(String title, String text) {
        if (!(text == null || text.isEmpty()) && containsAny(text, REDIRECT_TERMS))
            return REDIRECT;
        if (!(text == null || text.isEmpty()) && containsAny(text, DISAMBIGUATION_TERMS)
                || !(title == null || title.isEmpty()) && containsAny(title, TITLE_DISAMBIGUATION_TERMS))
            return DISAMBIGUATION;
        return ARTICLE;
    }

    private static boolean containsAny(String text, String[] words) {
        String lowerCaseText = text.toLowerCase();
        for(String ele : words) {
            if(lowerCaseText.contains(ele.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
