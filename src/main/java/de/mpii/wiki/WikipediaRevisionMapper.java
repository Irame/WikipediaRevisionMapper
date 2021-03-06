package de.mpii.wiki;

import de.mpii.wiki.dump.DumpData;
import de.mpii.wiki.dump.DumpReader;
import de.mpii.wiki.dump.DumpType;
import de.mpii.wiki.result.MappedResult;
import de.mpii.wiki.result.MappedResults;
import de.mpii.wiki.result.MappedType;
import de.mpii.wiki.result.ResultGenerator;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class consists of static methods that operate on either individual Wikipedia dump file or
 * on multiple versions of dumps.
 *
 * @author fkeller
 *         <p>
 *         Parts of this code are taken from https://github.com/vvenkatr/wiki-tools by vvenkatr.
 */
public class WikipediaRevisionMapper {

    private static Logger logger_ = LoggerFactory.getLogger(WikipediaRevisionMapper.class);

    private static boolean evalMode;


    /**
     * Returns Map of Wiki page titles from old dump to new dump. The map also includes page entries that
     * remain unchanged between the old and new dump. If any entry is deleted in the new dump, then the title
     * from old dump will be mapped to null.
     * <p>
     * In case of cyclic redirects, the old title will be mapped to itself.
     *
     * @param oldDump The old dump to verify.
     * @param newDump The new dump to compare with.
     * @return Map of old page titles to new page titles.
     * @throws IOException        if loading of dumps fail.
     * @throws XMLStreamException if dump xml is invalid.
     */
    public static Map<String, String> map(File oldDump, File newDump) throws IOException, XMLStreamException {
        return map(oldDump, newDump, true);
    }

    /**
     * Returns Map of Wiki page titles from old dump to new dump. If includeUnchangedEntries is false, unchanged
     * entries will not be added to the final map returned. If any entry is deleted in the new dump, then the title
     * from old dump will be mapped to null.
     * <p>
     * In case of cyclic redirects, the old title will be mapped to itself.
     *
     * @param oldDump                 The old dump to verify.
     * @param newDump                 The new dump to compare with.
     * @param includeUnchangedEntries Flag to include/exclude unchanged entries.
     * @return Map of old page titles to new page titles.
     * @throws IOException        if loading of dumps fail.
     * @throws XMLStreamException if dump xml is invalid.
     */

    public static Map<String, String> map(File oldDump, File newDump, boolean includeUnchangedEntries) throws IOException, XMLStreamException {
        MappedResults results = mapImpl(oldDump, newDump);
        Map<String, String> finalMap = new HashMap<>();
        for (MappedResult result : results.getResults()) {
            String source = result.getSourceTitle();
            String target = result.getTargetTitle();
            MappedType mapType = result.getMappingType();
            logger_.debug(source + "->" + target + "(" + mapType + ")");

            if (mapType.equals(MappedType.UNCHANGED) && !includeUnchangedEntries)
                continue;

            finalMap.put(source, target);
        }
        results.printResultStats();
        return finalMap;
    }

    /**
     * Writes the result of map method to file provided. The map also includes page entries that
     * remain unchanged between the old and new dump. If any entry is deleted in the new dump, then the title
     * from old dump will be mapped to null.
     * <p>
     * In case of cyclic redirects, the old title will be mapped to itself.
     *
     * @param oldDump The old dump to verify.
     * @param newDump The new dump to compare with.
     * @param output  The path to write the final results.
     * @throws IOException        if loading of dumps fail.
     * @throws XMLStreamException if dump xml is invalid.
     */
    public static void mapToFile(File oldDump, File newDump, File output) throws IOException, XMLStreamException {
        // by default, include the unchanged entries as well.
        mapToFile(oldDump, newDump, output, true);
    }

    /**
     * Writes the result of map method to file provided. If includeUnchangedEntries is false, unchanged
     * entries will not be added to the final map returned. If any entry is deleted in the new dump, then the title
     * from old dump will be mapped to null.
     * <p>
     * In case of cyclic redirects, the old title will be mapped to itself.
     *
     * @param oldDump The old dump to verify.
     * @param newDump The new dump to compare with.
     * @param output  The path to write the final results.
     * @throws IOException        if loading of dumps fail.
     * @throws XMLStreamException if dump xml is invalid.
     */
    public static void mapToFile(File oldDump, File newDump, File output, boolean includeUnchangedEntries) throws IOException, XMLStreamException {
        MappedResults result = mapImpl(oldDump, newDump);
        logger_.debug("Writing results to file : " + output.getName());
        try {
            List<MappedResult> results = result.getResults();

            if (!includeUnchangedEntries)
                results = results.stream().filter(mappedResult -> mappedResult.getMappingType() != MappedType.UNCHANGED).collect(Collectors.toList());

            if (evalMode)
                FileUtils.writeFileContentAsXML(output, results);
            else
                FileUtils.writeFileContent(output, results);

            logger_.debug(result.size() + " entries written to " + output.getName());
            result.printResultStats();
        } catch (IOException ioe) {
            logger_.error("Failed to write results to file");
        }
    }

    private static MappedResults mapImpl(File oldDump, File newDump) throws IOException, XMLStreamException {
        DumpData newDumpData = new DumpData(evalMode ? DumpType.TARGET_EVAL : DumpType.TARGET);
        DumpData oldDumpData = new DumpData(evalMode ? DumpType.SOURCE_EVAL : DumpType.SOURCE);

        long start = System.currentTimeMillis();
        logger_.info("Processing Target Dump...");
        DumpReader.read(newDump, newDumpData);
        logger_.info("Time to scan target dump : " + (System.currentTimeMillis() - start) / 1000 + " s.");

        // iterate over the source dump
        start = System.currentTimeMillis();
        logger_.info("Processing Source Dump...");
        DumpReader.read(oldDump, oldDumpData);
        logger_.info("Time to scan source dump : " + (System.currentTimeMillis() - start) / 1000 + " s.");

        return ResultGenerator.generate(oldDumpData, newDumpData);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private static Options buildCommandLineOptions() throws ParseException {
        Options options = new Options();
        options.addOption(OptionBuilder
                .withLongOpt("source")
                .withDescription("Old dump to be mapped")
                .hasArg()
                .isRequired()
                .withArgName("SOURCE_DUMP")
                .create("s"));
        options.addOption(OptionBuilder
                .withLongOpt("target")
                .withDescription("New dump to check against")
                .hasArg()
                .isRequired()
                .withArgName("TARGET_DUMP")
                .create("t"));
        options.addOption(OptionBuilder
                .withLongOpt("output")
                .withDescription("Write to file")
                .hasArg()
                .withArgName("FILENAME")
                .create("w"));
        options.addOption(OptionBuilder
                .withLongOpt("evaluate")
                .withDescription("Runs Mapper in evaluation mode - " +
                        "Stores snippet of page texts for manual verification of disambiguations")
                .create("e"));
        options.addOption(OptionBuilder.withLongOpt("help").create('h'));
        return options;
    }

    private static void printHelp(Options commandLineOptions) {
        String header = "\n\nWikipediaRevisionMapper:\n\n";
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

        String srcDump = cmd.getOptionValue('s');
        String tgtDump = cmd.getOptionValue('t');

        evalMode = cmd.hasOption('e');
        if (evalMode)
            logger_.info(">>> Evaluation Mode <<<");

        if (cmd.hasOption('w')) {
            File outputFile = new File(cmd.getOptionValue('w'));
            if (outputFile.exists()) {
                logger_.error("Output file already exists : " + outputFile.getName() + ". Re-run after deleting/moving the file");
                return;
            }
            mapToFile(new File(srcDump), new File(tgtDump), outputFile);
        } else {
            MappedResults results = mapImpl(new File(srcDump), new File(tgtDump));
            FileUtils.writeFileContent(null, results.getResults());
            results.printResultStats();
        }
    }
}
