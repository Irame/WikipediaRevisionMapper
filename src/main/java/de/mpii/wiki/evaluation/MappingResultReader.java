package de.mpii.wiki.evaluation;

import de.mpii.wiki.WikipediaRevisionMapper;
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
import java.util.List;

public class MappingResultReader {
    public static List<MappedResult> read(File file) throws FileNotFoundException, XMLStreamException {
        List<MappedResult> results = new ArrayList<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new FileReader(file));

        MappedType mappedType = null;

        int srcId = -1;
        String srcTitle = null;
        String srcText = null;

        int tgtId = -1;
        String tgtTitle = null;
        String tgtText = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                String elementName = event.asStartElement().getName().getLocalPart();

                if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_ENTRY_TAG)) {
                    mappedType = null;
                    srcId = -1;
                    srcTitle = null;
                    srcText = null;
                    tgtId = -1;
                    tgtTitle = null;
                    tgtText = null;
                    continue;
                }
                String content = reader.nextEvent().asCharacters().getData();
                if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_MAPPING_TYPE_TAG)) {
                    mappedType = MappedType.valueOf(content);
                } else if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_SOURCE_ID_TAG)) {
                    srcId = Integer.parseInt(content);
                } else if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_SOURCE_TITLE_TAG)) {
                    srcTitle = content;
                } else if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_SOURCE_TEXT_TAG)) {
                    srcText = content;
                } else if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_TARGET_ID_TAG)) {
                    tgtId = Integer.parseInt(content);
                } else if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_TARGET_TITLE_TAG)) {
                    tgtTitle = content;
                } else if (elementName.equals(WikipediaRevisionMapper.EVAL_XML_TARGET_TEXT_TAG)) {
                    tgtText = content;
                }
            } else if (event.isEndElement()) {
                if (event.asEndElement().getName().getLocalPart().equals(WikipediaRevisionMapper.EVAL_XML_ENTRY_TAG)) {
                    results.add(new MappedResult(srcId, srcTitle, srcText, tgtId, tgtTitle ,tgtText, mappedType));
                }
            }
        }
        return results;
    }
}
