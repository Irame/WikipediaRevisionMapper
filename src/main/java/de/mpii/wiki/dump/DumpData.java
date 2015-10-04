package de.mpii.wiki.dump;

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

    // number of page entries processed
    private int processedPages = 0;

    private static Logger logger_ = LoggerFactory.getLogger(DumpData.class);

    private void init() {
        idTitleMap = new TIntObjectHashMap<>();
        titleIdMap = new TObjectIntHashMap<>();

        disambiguations = new TIntObjectHashMap<>();
        redirections = new TIntObjectHashMap<>();
        articleLinks = new TIntObjectHashMap<>();
    }

    public DumpData(DumpType dType) {
        init();
        dumpType = dType;
    }

    public void addPageEntry(int id, String title) {
        idTitleMap.put(id, title);
        titleIdMap.put(title, id);
    }

    public void addContentInfo(int id, String content) {
        if(!idTitleMap.containsKey(id))
            throw new IllegalArgumentException("id '" + id + "' does not exist (add the id first by calling 'addPageEntry()')");

        PageType pageType = PageType.parseFrom(idTitleMap.get(id), content);

        // store redirections and disambiguation only for target dump
        TIntList links = extractLinks(content);
        if (dumpType == DumpType.TARGET && pageType.isSpecialPage()) {
            if (pageType == PageType.REDIRECT) {
                redirections.put(id, links);
            } else { // pageType == PageType.DISAMBIGUATION
                disambiguations.put(id, links);
            }
        } else if (pageType == PageType.ARTICLE) {
            articleLinks.put(id, links);
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


    private int disambiguate(int srcPageId, TIntList srcPageLinks) {
        TIntList tgtPageDisambiguationLinks = disambiguations.get(srcPageId);

        double maxScore = 0.0;
        int result = srcPageId; // return the current pageId, if no disambiguations are found

        if (tgtPageDisambiguationLinks == null || tgtPageDisambiguationLinks.isEmpty())
            return result;

        // for each disambiguation option, get the content stored in pageContent and compute similarity
        TIntIterator iterator = tgtPageDisambiguationLinks.iterator();
        while (iterator.hasNext()) {
            int tgtPageId = iterator.next();
            TIntList tgtPageLinks = articleLinks.get(tgtPageId);
            double score = 0; //TODO: compute the score in a good way (input: srcPageLinks, tgtPageLinks [, tgtPageId])
            //logger_.debug("Target Disambiguation Page : "+ tgtPageTitle + " with score : " + score);
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
                return itK;
            }

            //FIXME: if tmp size is greater than 1, then something is wrong with the redirect page : Not handled!
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
            linkIds.add(titleIdMap.get(redirectMatcher.group(1)));
        }
        return linkIds;
    }
}