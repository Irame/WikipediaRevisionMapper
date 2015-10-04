package de.mpii.wiki.dump;

import de.mpii.wiki.TestHelpers;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

public class PageTypeTest {

    @Test
    public void verifyForTextContainingGivenWords() {
        String strToVerify = "a quick brown fox jumped over the lazy dog";
        String[][] lookUpWords = new String[][]{{"Quick"}, {"QUICK"}, {"Brown"}, {"LAzY"}, {"slow"}, {"under"}};
        boolean[] outcomes = new boolean[]{true, true, true, true, false, false};

        for (int i = 0; i < lookUpWords.length; i++) {
            String[] words = lookUpWords[i];
            boolean outcome = outcomes[i];
            try {
                assertEquals(outcome, TestHelpers.invokePrivateStaticMethod(PageType.class, "containsAny", strToVerify, words));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Test
    public void identifyRedirectPageFromContent() {
        PageType pageType = PageType.parseFrom("Test", "#REDIRECT [[Test3]] {{R from CamelCase}}");

        assertEquals(PageType.REDIRECT, pageType);
    }

    @Test
    public void identifyDisambiguationPageFromContent() {
        PageType pageType = PageType.parseFrom("Test", "'''Einstein''' may refer to:* [[Einstein (crater)]], a large lunar crater along the Moons west limb* [[Einstein Observatory]], tje forst first fully imaging X-ray telescope put into orbit* [[Einstein Tower]], an astrophysical observatory in Potsdan, Germany __NOTOC__ {{Disambig}}");
        assertEquals(PageType.DISAMBIGUATION, pageType);

        pageType = PageType.parseFrom("Test", "{{Wiktionary|gross|groß}}'''Gross''' may refer to:*[[Gross (economics)]], before deductions*[[Gross (unit)]], a counting unit equal to 144*[[Gross weight]]*[[Gross, Nebraska]], a US village*[[Gross!]], a television show on Discovery Channel* A [[colloquialism]] meaning [[disgust]]ing.People with the surname '''Gross''':*[[Gross (surname)]]*''See also'' [[Grosz]]==See also==*[[Gross examination]], in anatomical pathology, identification of disease with the naked eye*[[Gross anatomy]], macroscopic anatomy*[[Gross indecency]], in law, flagrant indecency*[[Gross negligence]], in law, flagrant negligence*[[Daniel J. Gross Catholic High School]], Omaha Gross High School {{disambiguation}} [[cs:Gross]][[de:Groß]][[fr:Gross]][[he:גרוס]][[lv:Gross]][[ja:グロス]][[pt:Gross]][[ru:Гросс]]");
        assertEquals(PageType.DISAMBIGUATION, pageType);
    }

    @Test
    public void identifyDisambiguationPageFromTitle() {
        PageType pageType = PageType.parseFrom("Montanaro (surname)", "'''Montanaro''' is the last name of several people:*[[Donato A. Montanaro]], co-founder, chairman and CEO of online brokerage house TradeKing*[[Tony Montanaro]], mime artist*[[Lucio Montanaro]], Italian actor{{surname|Montanaro}}*[[Domingo M. Montanaro]] Nice Guy!");
        assertEquals(PageType.DISAMBIGUATION, pageType);

        pageType = PageType.parseFrom("Title (Disambiguation)", "'''Einstein''' may refer to:* [[Einstein (crater)]], a large lunar crater along the Moons west limb* [[Einstein Observatory]], tje forst first fully imaging X-ray telescope put into orbit* [[Einstein Tower]], an astrophysical observatory");
        assertEquals(PageType.DISAMBIGUATION, pageType);
    }

    @Test
    public void identifyNormalPageWithDisambiguationMarkerInBracket() {
        PageType pageType = PageType.parseFrom("Title", "Some times an entity page contains (disambiguation) even though it is a normal page");

        assertEquals(PageType.ARTICLE, pageType);
    }
}