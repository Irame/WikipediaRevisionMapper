package de.mpii.wiki.result;

import de.mpii.wiki.dump.DumpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultGenerator {

    private static Logger logger_ = LoggerFactory.getLogger(ResultGenerator.class);

    public static MappedResults generate(DumpData sourceData, DumpData targetData) {
        MappedResults results = new MappedResults();

        int[] srcIds = sourceData.getPageIds();
        for (int srcId : srcIds) {
            int tgtId = srcId;

            MappedType type;

            String srcTitle;
            String tgtTitle;

            String srcText = null;
            String tgtText = null;

            srcTitle = sourceData.getTitle(srcId);
            
            boolean redirectedSource = false;

            if (sourceData.isDisambiguation(srcId)) {
                // Source entry is either redirect/disambiguation and ignored
                type = MappedType.SOURCE_IGNORED;
                if (targetData.hasId(srcId)) {
                    tgtTitle = targetData.getTitle(srcId);
                } else {
                    tgtTitle = srcTitle;
                }
            } else {
                if (sourceData.isRedirect(srcId)) {
                    srcId = sourceData.getRedirectedId(srcId);
                    srcTitle = sourceData.getTitle(srcId);
                    redirectedSource = true;
                }
                if (targetData.isRedirect(srcId)) {
                    // source id is valid, check target for redirections
                    tgtId = targetData.getRedirectedId(srcId);
                    if (tgtId == srcId) {
                        type = MappedType.REDIRECTED_CYCLE;
                    } else {
                        type = MappedType.REDIRECTED;
                    }

                    tgtTitle = targetData.getTitle(tgtId);
                    logger_.debug(srcTitle + "(" + srcId + ") redirects to : " + tgtTitle + "(" + tgtId + ")");
                } else if (targetData.isDisambiguation(srcId)) {
                    // not a redirection, verifying for disambiguation
                    if (redirectedSource) {
                        type = MappedType.REDIRECTED_DISAMBIGUATED;
                    } else {
                        type = MappedType.DISAMBIGUATED;
                    }
                    tgtId = targetData.getDisambiguatedId(srcId, sourceData.getPageLinks(srcId));
                    tgtTitle = targetData.getTitle(tgtId);
                    srcText = sourceData.getPageText(srcId);
                    tgtText = targetData.getPageText(tgtId);
                    logger_.info(srcTitle + "(" + srcId + ") disambiguates to : " + tgtTitle + "(" + tgtId + ")");
                } else if (!targetData.hasId(srcId)) {
                    type = MappedType.DELETED;
                    tgtTitle = null;
                    tgtId = -1;
                } else {
                    // if not any of above, check whether it has been updated/unchanged!
                    tgtTitle = targetData.getTitle(srcId);
                    if (!srcTitle.equals(tgtTitle)) {
                        type = MappedType.UPDATED;
                    } else {
                        // A valid source id that is not deleted, updated, redirected or disambiguated in target is an Unchanged entry
                        type = MappedType.UNCHANGED;
                    }
                }
            }
            results.add(new MappedResult(srcId, srcTitle, srcText, tgtId, tgtTitle, tgtText, type));
        }
        return results;
    }
}