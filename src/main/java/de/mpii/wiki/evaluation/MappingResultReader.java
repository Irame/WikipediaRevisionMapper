package de.mpii.wiki.evaluation;

import de.mpii.wiki.FileUtils;
import de.mpii.wiki.result.MappedResult;
import de.mpii.wiki.result.MappedType;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MappingResultReader {
    public static List<MappedResult> read(File file, Collection<MappedType> typesToRead) throws FileNotFoundException, XMLStreamException {
        List<MappedResult> results = new ArrayList<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new FileReader(file));

        MappedType mappedType = null;

        int srcId = -1;
        String srcTitle = null;
        StringBuilder srcText = new StringBuilder();

        int tgtId = -1;
        String tgtTitle = null;
        StringBuilder tgtText = new StringBuilder();

        int entriesProcessed = 0;
        String elementName = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                elementName = event.asStartElement().getName().getLocalPart();
                if (elementName.equals(FileUtils.EVAL_XML_ENTRY_TAG)) {
                    mappedType = null;
                    srcId = -1;
                    srcTitle = null;
                    srcText = new StringBuilder();
                    tgtId = -1;
                    tgtTitle = null;
                    tgtText = new StringBuilder();
                }
            } else if (mappedType != null && !typesToRead.contains(mappedType)) {
                elementName = null;
            } else if (event.isCharacters()) {
                if (elementName == null) continue;
                String content = event.asCharacters().getData();
                if (elementName.equals(FileUtils.EVAL_XML_MAPPING_TYPE_TAG)) {
                    mappedType = MappedType.valueOf(content);
                } else if (elementName.equals(FileUtils.EVAL_XML_SOURCE_ID_TAG)) {
                    srcId = Integer.parseInt(content);
                } else if (elementName.equals(FileUtils.EVAL_XML_SOURCE_TITLE_TAG)) {
                    srcTitle = content;
                } else if (elementName.equals(FileUtils.EVAL_XML_SOURCE_TEXT_TAG)) {
                    srcText.append(content);
                } else if (elementName.equals(FileUtils.EVAL_XML_TARGET_ID_TAG)) {
                    tgtId = Integer.parseInt(content);
                } else if (elementName.equals(FileUtils.EVAL_XML_TARGET_TITLE_TAG)) {
                    tgtTitle = content;
                } else if (elementName.equals(FileUtils.EVAL_XML_TARGET_TEXT_TAG)) {
                    tgtText.append(content);
                }
            } else if (event.isEndElement()) {
                elementName = null;
                if (event.asEndElement().getName().getLocalPart().equals(FileUtils.EVAL_XML_ENTRY_TAG)) {
                    results.add(new MappedResult(srcId, srcTitle, srcText.length() == 0 ? null : srcText.toString(), tgtId, tgtTitle, tgtText.length() == 0 ? null : tgtText.toString(), mappedType));
                    if (++entriesProcessed % 100_000 == 0)
                        System.out.format("Entries processed: %d k\n", entriesProcessed/1000);
                }
            }
        }
        return results;
    }
}
