package de.mpii.wiki.dump;

import de.mpii.wiki.compute.DisambiguationScoreCalculator;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DumpData {
    private static final int TINTMAP_NO_ENTRY_VALUE = -1;

    private final DumpType dumpType;

    private static final Pattern linkPattern = Pattern.compile("\\[\\[(.*?)(?:\\]\\]|\\|)");

  /*
   * Basic information regarding a page in wiki dump
   */

    // pageId -> pageTitle map
    private TIntObjectMap<String> idTitleMap;

    // pageTitle -> pageId map
    private TObjectIntMap<String> titleIdMap;

  /*
   * Additional information for entries in dump
   */

    // Stores Page Id and list of titles to which the page disambiguates to
    private TIntObjectMap<TIntList> disambiguations;

    // Stores Redirections (id -> id map)
    private TIntObjectMap<TIntList> redirections;

    // Stores link on a Wikipedia article
    private TIntObjectMap<TIntList> articleLinks;

    private TIntObjectMap<String> pageText;

    private static Logger logger_ = LoggerFactory.getLogger(DumpData.class);

    private void init() {
        idTitleMap = new TIntObjectHashMap<>();
        titleIdMap = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, TINTMAP_NO_ENTRY_VALUE);

        disambiguations = new TIntObjectHashMap<>();
        redirections = new TIntObjectHashMap<>();
        articleLinks = new TIntObjectHashMap<>();

        pageText = new TIntObjectHashMap<>();
    }

    public DumpData(DumpType dType) {
        init();
        dumpType = dType;
    }

    public void addPageEntry(int id, String title) {
        idTitleMap.put(id, title);
        titleIdMap.put(title.toLowerCase(), id);
    }

    public void addContentInfo(int id, String content) {
        if(!idTitleMap.containsKey(id))
            throw new IllegalArgumentException("id '" + id + "' does not exist (add the id first by calling 'addPageEntry()')");

        PageType pageType = PageType.parseFrom(idTitleMap.get(id), content);

        // store disambiguation only for target dump
        TIntList links = extractLinks(content);
        if (pageType == PageType.DISAMBIGUATION) {
            disambiguations.put(id, links);
        } else if (pageType == PageType.ARTICLE) {
            articleLinks.put(id, links);
        } else if (pageType == PageType.REDIRECT) {
            redirections.put(id, links);
        }

        if (dumpType.isEval() && (
                (dumpType.isTarget() && pageType == PageType.DISAMBIGUATION)
             || (dumpType.isSource() && pageType == PageType.ARTICLE))) {
            pageText.put(id, content);
        }
    }

    public int size() {
        return idTitleMap.size();
    }

    public int[] getPageIds() {
        return idTitleMap.keys();
    }

    public String getTitle(int id) {
        return idTitleMap.get(id);
    }

    public boolean isRedirect(int id) {
        return redirections.containsKey(id);
    }

    public int getRedirectedId(int id) {
        return resolveRedirection(id);
    }

    public boolean isDisambiguation(int id) {
        return disambiguations.containsKey(id);
    }

    public int getDisambiguatedId(int id, TIntList links) {
        return disambiguate(id, links);
    }

    public boolean isArticle(int id) { return articleLinks.containsKey(id); }

    public TIntList getPageLinks(int pId) {
        return articleLinks.get(pId);
    }

    public boolean hasId(int id) {
        return idTitleMap.containsKey(id);
    }

    public String getPageText(int id) {
        return pageText.get(id);
    }

    private int disambiguate(int srcPageId, TIntList srcPageLinks) {
        TIntList tgtPageDisambiguationLinks = disambiguations.get(srcPageId);

        double maxScore = -1.0;
        int result = srcPageId; // return the current pageId, if no disambiguations are found

        if (tgtPageDisambiguationLinks == null || tgtPageDisambiguationLinks.isEmpty())
            return result;

        double linkPositionOnPage = 1;

        // for each disambiguation option, get the content stored in pageContent and compute similarity
        TIntIterator iterator = tgtPageDisambiguationLinks.iterator();
        while (iterator.hasNext()) {
            int tgtPageId = iterator.next();
            double score = DisambiguationScoreCalculator.compute(articleLinks.get(tgtPageId), srcPageLinks);
            score /= linkPositionOnPage++;
            logger_.debug("Target Disambiguation Page : "+ tgtPageId + " with score : " + score);
            if (score > maxScore) {
                result = tgtPageId;
                maxScore = score;
            }
        }
        return result;
    }

    //This method resolves redirection pages(including multiple redirections).
    //  In case of a cycle, the given id is returned i.e id is mapped on to itself.
    private int resolveRedirection(int redirectId) {
        TIntSet processed = new TIntHashSet();
        processed.add(redirectId);
        int itK = redirectId;
        boolean found = false;
        while (redirections.containsKey(itK)) {

            TIntList tmp = redirections.get(itK);

            if (tmp == null || tmp.isEmpty()) {
                logger_.warn("Redirect page '{}' (id: {}) has no valid redirection", idTitleMap.get(redirectId), redirectId);
                return itK;
            }

            if (tmp.size() > 1)
                logger_.warn("Redirect page '{}' (id: {}) has more than one link (taking the first one)", idTitleMap.get(redirectId), redirectId);

            itK = tmp.get(0);

            if (!processed.contains(itK)) {
                processed.add(itK);
            } else {
                logger_.warn("Cycle Found for id : " + redirectId + ": " + processed);
                // redirectionCyclesPageIds.add(redirectId);
                found = true;
                break;
            }
        }
        if (found) return redirectId;
        return itK;
    }

    private TIntList extractLinks(String content) {
        TIntList linkIds = new TIntArrayList();
        if (content == null || content.equals("")) {
            return linkIds;
        }

        Matcher redirectMatcher = linkPattern.matcher(content);
        while (redirectMatcher.find()) {
            int linkId = titleIdMap.get(redirectMatcher.group(1).toLowerCase());
            if (linkId != TINTMAP_NO_ENTRY_VALUE)
                linkIds.add(linkId);
        }
        return linkIds;
    }
}