package de.mpii.wiki;

import de.mpii.wiki.result.MappedResult;
import de.mpii.wiki.result.MappedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileUtils {
    private static Logger logger_ = LoggerFactory.getLogger(FileUtils.class);
    
    public static final String EVAL_XML_MAPPING_RESULT_TAG = "MappingResult";
    public static final String EVAL_XML_ENTRY_TAG = "Entry";
    public static final String EVAL_XML_MAPPING_TYPE_TAG = "MappingType";
    public static final String EVAL_XML_SOURCE_ID_TAG = "SourceId";
    public static final String EVAL_XML_SOURCE_TITLE_TAG = "SourceTitle";
    public static final String EVAL_XML_SOURCE_TEXT_TAG = "SourceText";
    public static final String EVAL_XML_TARGET_ID_TAG = "TargetId";
    public static final String EVAL_XML_TARGET_TITLE_TAG = "TargetTitle";
    public static final String EVAL_XML_TARGET_TEXT_TAG = "TargetText";

    public static void writeFileContent(File file, List<MappedResult> results) throws IOException {
        BufferedWriter writer;
        if (file == null) {
            // write to standard output
            writer = new BufferedWriter(new OutputStreamWriter(System.out));
        } else {
            // need to append entries to the file
            writer = new BufferedWriter(new FileWriter(file, true));
        }

        for (MappedResult result : results) {
            String srcTitle = result.getSourceTitle();
            String tgtTitle = result.getTargetTitle();
            MappedType mapType = result.getMappingType();
            writer.append(titleToWikiLink(srcTitle)).append("\t")
                .append(titleToWikiLink(tgtTitle)).append("\t")
                .append(mapType.toString());

            writer.append("\n");
        }

        writer.flush();
        writer.close();
    }

    private static String titleToWikiLink(String title) throws UnsupportedEncodingException {
        return "http://en.wikipedia.org/wiki/" + URLEncoder.encode(title, StandardCharsets.UTF_8.name()).replace("+", "_");
    }

    public static void writeFileContentAsXML(File file, List<MappedResult> results) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer;

        try {
            if (file == null) {
                // write to standard output
                writer = factory.createXMLStreamWriter(new OutputStreamWriter(System.out));
            } else {
                // need to append entries to the file
                writer = factory.createXMLStreamWriter(new FileWriter(file));
            }

            String sourceText;
            String targetText;

            writer.writeStartDocument();
            writer.writeStartElement(EVAL_XML_MAPPING_RESULT_TAG);
            for (MappedResult result : results) {
                writer.writeStartElement(EVAL_XML_ENTRY_TAG);
                writer.writeStartElement(EVAL_XML_MAPPING_TYPE_TAG);
                writer.writeCharacters(result.getMappingType().toString());
                writer.writeEndElement();
                writer.writeStartElement(EVAL_XML_SOURCE_ID_TAG);
                writer.writeCharacters(String.valueOf(result.getSourceId()));
                writer.writeEndElement();
                writer.writeStartElement(EVAL_XML_SOURCE_TITLE_TAG);
                writer.writeCharacters(result.getSourceTitle());
                writer.writeEndElement();
                if ((sourceText = result.getSourceText()) != null) {
                    writer.writeStartElement(EVAL_XML_SOURCE_TEXT_TAG);
                    writer.writeCharacters(sourceText);
                    writer.writeEndElement();
                }
                writer.writeStartElement(EVAL_XML_TARGET_ID_TAG);
                writer.writeCharacters(String.valueOf(result.getTargetId()));
                writer.writeEndElement();
                writer.writeStartElement(EVAL_XML_TARGET_TITLE_TAG);
                writer.writeCharacters(result.getTargetTitle());
                writer.writeEndElement();
                if ((targetText = result.getTargetText()) != null) {
                    writer.writeStartElement(EVAL_XML_TARGET_TEXT_TAG);
                    writer.writeCharacters(targetText);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndDocument();

            writer.flush();
            writer.close();
        } catch (XMLStreamException | IOException e) {
            logger_.error("Writing to XML failed", e);
        }
    }
}
