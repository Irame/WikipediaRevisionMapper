package de.mpii.wiki.evaluation;

import de.mpii.wiki.result.MappedResult;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by Felix on 09.10.2015.
 */
public class WikipediaMappingEvaluator {

    /**
     * Computes the Wilson Interval (see
     * http://en.wikipedia.org/wiki/Binomial_proportion_confidence_interval
     * #Wilson_score_interval) Given the total number of events and the number
     * of "correct" events, returns in a double-array in the first component the
     * center of the Wilson interval and in the second component the width of
     * the interval. alpha=95%.
     */
    public static double[] wilson(int total, int correct) {
        double z = 1.96;
        double p = (double) correct / total;
        double center = (p + 1 / 2.0 / total * z * z)
                / (1 + 1.0 / total * z * z);
        double d = z
                * Math.sqrt((p * (1 - p) + 1 / 4.0 / total * z * z) / total)
                / (1 + 1.0 / total * z * z);
        return (new double[] { center, d });
    }

    public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
        if (args.length != 1) {
            System.out.println("Usage: WikipediaMappingEvaluator <resultFile>");
            System.exit(1);
        }
        File resultFile = new File(args[1]);

        List<MappedResult> results = MappingResultReader.read(resultFile);
        List<MappedResult> correctResults = new ArrayList<>();
        List<MappedResult> incorrectResults = new ArrayList<>();

        Set<String> acceptInput = new HashSet<>(Arrays.asList("yes", "y", "+"));
        Set<String> declineInput = new HashSet<>(Arrays.asList("no", "n", "-"));

        double[] wilsonResult;

        Random random = new Random(System.currentTimeMillis());
        Scanner scanner = new Scanner(System.in);
        System.out.format(">> START (file: %s) <<<\n", resultFile.getAbsolutePath());
        do {
            MappedResult curMappedResult = results.remove(random.nextInt(results.size()));
            System.out.format(
                    ">>> MappingType: %s\n" +
                    ">>> Ids: %d => %d\n" +
                    ">>> Titles: '%s' => '%s'\n",
                    curMappedResult.getMappingType(),
                    curMappedResult.getSourceId(), curMappedResult.getTargetId(),
                    curMappedResult.getSourceTitle(), curMappedResult.getTargetTitle());
            if (curMappedResult.getSourceText() != null)
                System.out.format(">>> Source Text:\n%s\n", curMappedResult.getSourceText());
            if (curMappedResult.getTargetText() != null)
                System.out.format(">>> Target Text:\n%s\n", curMappedResult.getTargetText());

            System.out.println(">>> Is this mapping correct?");
            String userInput = scanner.next();
            if (acceptInput.contains(userInput.toLowerCase())) {
                correctResults.add(curMappedResult);
                System.out.println(">>> Tagged as CORRECT");
            } else if (declineInput.contains(userInput.toLowerCase())) {
                incorrectResults.add(curMappedResult);
                System.out.println(">>> Tagged as INCORRECT");
            } else {
                System.out.println(">>> Skipped");
            }
            wilsonResult = wilson(correctResults.size() + incorrectResults.size(), correctResults.size());
            System.out.format(">>> Wilson Intervall: Center = %.2f, Distance = %.2f\n", wilsonResult[0], wilsonResult[1]);
        } while (wilsonResult[1] > 0.05);
        System.out.println(">>> DONE <<<");
    }
}
