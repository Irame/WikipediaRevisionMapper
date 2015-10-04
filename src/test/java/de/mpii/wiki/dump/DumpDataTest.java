package de.mpii.wiki.dump;

import de.mpii.wiki.TestHelpers;
import gnu.trove.list.TIntList;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class DumpDataTest {

    @Test
    public void verifyLinksExtractedFromWikiPages() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String text = "[[Albert_Einstein|Albert Einstein]] was a German-American theoretical physicist. Einstein was born in [[Ulm_Link|Ulm]], [[Germany_Link|Germany]]. He developed [[general_theory_of_relativity|general theory of relativity]]. He was awarded Nobel prize in Physics in 1921.";

        String[] linksToCheck = new String[]{"Albert Einstein", "Albert_Einstein", "Einstein", "Ulm_Link", "Ulm", "Germany_Link", "Germany", "general_theory_of_relativity", "general theory of relativity"};
        boolean[] outcomes = new boolean[]{false, true, false, true, false, true, false, true, false};

        DumpData dumpData = new DumpData(DumpType.SOURCE);

        for (int i = 0; i < linksToCheck.length; i++) {
            dumpData.addPageEntry(i, linksToCheck[i]);
        }

        TIntList links = TestHelpers.invokePrivateMethod(dumpData, "extractLinks", text);

        for (int i = 0; i < linksToCheck.length; i++) {
            boolean outcome = outcomes[i];
            assert links.contains(i) == outcome;
        }
    }
}
