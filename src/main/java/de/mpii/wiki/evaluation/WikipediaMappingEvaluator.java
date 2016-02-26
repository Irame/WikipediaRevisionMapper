package de.mpii.wiki.evaluation;

import de.mpii.wiki.FileUtils;
import de.mpii.wiki.result.MappedResult;
import de.mpii.wiki.result.MappedType;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.util.*;

public class WikipediaMappingEvaluator {

    private static Logger logger = LoggerFactory.getLogger(WikipediaMappingEvaluator.class);
    
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
    
    private static String capString(String s, int length) {
        return s.length() > length ? s.substring(0, length) : s;
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private static Options buildCommandLineOptions() throws ParseException {
        Options options = new Options();
        options.addOption(OptionBuilder
            .withLongOpt("input")
            .withDescription("Mapping results to evaluate")
            .hasArg()
            .isRequired()
            .withArgName("INPUT_FILE")
            .create("i"));
        options.addOption(OptionBuilder
            .withLongOpt("types")
            .withDescription("Mapping types to evaluate")
            .hasArg()
            .withArgName("MAPPING_TYPES")
            .create("t"));
        options.addOption(OptionBuilder
            .withLongOpt("output-incorrect")
            .withDescription("Output file for incorrect mappings")
            .hasArg()
            .withArgName("OUTPUT_INCORRECT_FILE")
            .create("oi"));
        options.addOption(OptionBuilder
            .withLongOpt("output-correct")
            .withDescription("Output file for correct mappings")
            .hasArg()
            .withArgName("OUTPUT_CORRECT_FILE")
            .create("oc"));
        options.addOption(OptionBuilder.withLongOpt("help").create('h'));
        return options;
    }
    
    private static void printHelp(Options commandLineOptions) {
        String header = "\n\nWikipediaRevisionMapperEvaluator:\n\n";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("WikiTools", header,
            commandLineOptions, "", true);
        System.exit(0);
    }
    
    public static void main(String[] args) throws Exception {
        Options commandLineOptions = buildCommandLineOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(commandLineOptions, args);
        } catch (MissingOptionException e) {
            System.out.println("\n\n" + e + "\n\n");
            printHelp(commandLineOptions);
            return;
        }
        if (cmd.hasOption("h")) {
            printHelp(commandLineOptions);
        }
        
        File resultFile = new File(cmd.getOptionValue("i"));
        
        Set<MappedType> typesToEvaluate;
        if (cmd.hasOption("t")) {
            typesToEvaluate = new HashSet<>();
            for (String typeString : cmd.getOptionValue("t").split(",")) {
                typesToEvaluate.add(MappedType.valueOf(typeString));
            }
        } else {
            typesToEvaluate = new HashSet<>(Arrays.asList(MappedType.values()));
        }

        File correctOutFile = null;
        if (cmd.hasOption("oc"))
            correctOutFile = new File(cmd.getOptionValue("oc"));
        else
            correctOutFile = new File(resultFile.getParentFile(), resultFile.getName() + ".correct");

        File incorrectOutFile = null;
        if (cmd.hasOption("oi"))
            incorrectOutFile = new File(cmd.getOptionValue("oi"));
        else
            incorrectOutFile = new File(resultFile.getParentFile(), resultFile.getName() + ".incorrect");

        logger.info("Start reading '{}' ...", resultFile.getName());
        List<MappedResult> results = MappingResultReader.read(resultFile, typesToEvaluate);
        logger.info("Finished reading '{}'!", resultFile.getName());
        List<MappedResult> correctResults = new ArrayList<>();
        List<MappedResult> incorrectResults = new ArrayList<>();

        Set<String> acceptInput = new HashSet<>(Arrays.asList("yes", "y", "+"));
        Set<String> declineInput = new HashSet<>(Arrays.asList("no", "n", "-"));

        double[] wilsonResult;
        List<MappedResult> curResultAsList = new ArrayList<MappedResult>(1){{
            add(null);
        }};
        int counter = 0;
        Random random = new Random(System.currentTimeMillis());
        Scanner scanner = new Scanner(System.in);
        logger.info(">>>> START (file: {})", resultFile.getAbsolutePath());
        do {
            MappedResult curMappedResult = results.remove(random.nextInt(results.size()));
            logger.info(">>> Evaluating Entry: {}", ++counter);
            
            String srcText = curMappedResult.getSourceText();
            if (srcText != null)
                logger.info(">> Source Text:\n{}\n\n", capString(srcText, 1000));
            
            String tgtText = curMappedResult.getTargetText();
            if (tgtText != null)
                logger.info(">> Target Text:\n{}\n\n", capString(tgtText, 1000));
            
            logger.info(">> MappingType: {}", curMappedResult.getMappingType());
            logger.info(">> Ids: {} => {}", curMappedResult.getSourceId(), curMappedResult.getTargetId());
            logger.info(">> Titles: '{}' => '{}'", curMappedResult.getSourceTitle(), curMappedResult.getTargetTitle());
            
            logger.info("> Is this mapping correct?");
            String userInput = scanner.next();
            curResultAsList.set(0, curMappedResult);
            if (acceptInput.contains(userInput.toLowerCase())) {
                correctResults.add(curMappedResult);
                FileUtils.writeFileContent(correctOutFile, curResultAsList);
                logger.info(">> Tagged as CORRECT");
            } else if (declineInput.contains(userInput.toLowerCase())) {
                incorrectResults.add(curMappedResult);
                FileUtils.writeFileContent(incorrectOutFile, curResultAsList);
                logger.info(">> Tagged as INCORRECT");
            } else {
                logger.info(">> Skipped");
            }
            wilsonResult = wilson(correctResults.size() + incorrectResults.size(), correctResults.size());
            logger.info(">> Wilson Intervall: Center = {}%, Distance = {}%", wilsonResult[0]*100, wilsonResult[1]*100);
        } while (correctResults.size() + incorrectResults.size() == 0 || wilsonResult[1] > 0.05);
        
        logger.info(">>>> DONE");
    }
}
