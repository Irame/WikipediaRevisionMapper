package de.mpii.wiki;

import de.mpii.wiki.result.MappedResult;
import de.mpii.wiki.result.MappedType;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class WikipediaRevisionMapperTest {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testUnchangedPageEntries() throws IOException, XMLStreamException {
        // source dump
        File tmpSrcDump = File.createTempFile("wiki-src-dump", "xml");
        File tmpTargetDump = File.createTempFile("wiki-target-dump", "xml");

        // base structure - "<mediawiki><page><title></title><id><id></page></mediawiki>"
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpSrcDump));
        bw.write("<mediawiki><page><title>Test1</title><id>1</id></page><page><title>Test2</title><id>2</id></page></mediawiki>");
        bw.close();

        bw = new BufferedWriter(new FileWriter(tmpTargetDump));
        bw.write("<mediawiki><page><title>Test1</title><id>1</id></page><page><title>Test2</title><id>2</id></page></mediawiki>");
        bw.close();

        // default diff will also include all unchanged entries
        Map<String, String> hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump);
        assertEquals(2, hshResults.size());

        // setting the flag to false will include unchanged entries
        hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump, false);
        assertEquals(0, hshResults.size());
        // remove tmp files
        tmpSrcDump.delete();
        tmpTargetDump.delete();
    }

    @Test
    public void testRenamedPageEntries() throws IOException, XMLStreamException {
        // source dump
        File tmpSrcDump = File.createTempFile("wiki-src-dump", "xml");
        File tmpTargetDump = File.createTempFile("wiki-target-dump", "xml");

        // base structure - "<mediawiki><page><title></title><id><id></page></mediawiki>"
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpSrcDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "</page>"
                + "<page>"
                + "<title>Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();

        bw = new BufferedWriter(new FileWriter(tmpTargetDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "</page>"
                + "<page>"
                + "<title>NEW_Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();

        // default diff will also include all unchanged entries
        Map<String, String> hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump);
        assertEquals(2, hshResults.size());

        // setting the flag to false will include unchanged entries
        hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump, false);
        assertEquals(1, hshResults.size());
        assertEquals(true, hshResults.containsKey("Test2"));
        assertEquals("NEW_Test2", hshResults.get("Test2"));

        tmpSrcDump.delete();
        tmpTargetDump.delete();
    }

    @Test
    public void testHandlingDeletedPageInTarget() throws IOException, XMLStreamException {
        // source dump
        File tmpSrcDump = File.createTempFile("wiki-src-dump", "xml");
        File tmpTargetDump = File.createTempFile("wiki-target-dump", "xml");

        // base structure - "<mediawiki><page><title></title><id><id></page></mediawiki>"
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpSrcDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "</page>"
                + "<page>"
                + "<title>Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();

        bw = new BufferedWriter(new FileWriter(tmpTargetDump));
        // deleted page with id 2 in the new dump -> mappig from id 2 in src shd be NULL
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();
        // setting the flag to false will include unchanged entries
        Map<String, String> hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump);
        assertEquals(true, hshResults.containsKey("Test2"));
        assertEquals(null, hshResults.get("Test2"));

        tmpSrcDump.delete();
        tmpTargetDump.delete();
    }

    @Test
    public void testSinglePageRedirection() throws IOException, XMLStreamException {
        // source dump
        File tmpOldDump = File.createTempFile("wiki-src-dump", "xml");
        File tmpNewDump = File.createTempFile("wiki-target-dump", "xml");

        // base structure - "<mediawiki><page><title></title><id><id></page></mediawiki>"
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpOldDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">This is entry about Test1 and it will be redirected.</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();

        bw = new BufferedWriter(new FileWriter(tmpNewDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "<redirect/>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">#REDIRECT [[Test3]] {{R from CamelCase}}</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>NEW_Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();
        Map<String, String> hshResults = WikipediaRevisionMapper.map(tmpOldDump, tmpNewDump);
        assertEquals(3, hshResults.size());
        assertEquals("Test3", hshResults.get("Test1"));

        tmpNewDump.delete();
        tmpOldDump.delete();
    }

    @Test
    public void testMultiplePageRedirection() throws IOException, XMLStreamException {
        // source dump
        File tmpSrcDump = File.createTempFile("wiki-src-dump", "xml");
        File tmpTargetDump = File.createTempFile("wiki-target-dump", "xml");

        // base structure - "<mediawiki><page><title></title><id><id></page></mediawiki>"
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpSrcDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">This is entry about Test1 and it will be redirected.</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test2</title>"
                + "<id>2</id>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">This is entry about Test2.</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">This is entry about Test3 and it will be redirected.</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test4</title>"
                + "<id>4</id>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">This is entry about Test4 and it will be redirected.</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test5</title>"
                + "<id>5</id>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">This is entry about Test5 and it will be redirected.</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test6</title>"
                + "<id>6</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();

        bw = new BufferedWriter(new FileWriter(tmpTargetDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "<redirect/>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">#REDIRECT [[Test3]] {{R from CamelCase}}</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>NEW_Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "<redirect/>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">#REDIRECT [[Test4]] {{R from CamelCase}}</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test4</title>"
                + "<id>4</id>"
                + "<redirect/>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">#REDIRECT [[Test5]] {{R from CamelCase}}</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test5</title>"
                + "<id>5</id>"
                + "<redirect/>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">#REDIRECT [[Test6]] {{R from CamelCase}}</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>Test6</title>"
                + "<id>6</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();
        Map<String, String> hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump);
        System.out.println("TEST MULTI PAGE REDIRECTION");
        System.out.println(hshResults);

        assertEquals(6, hshResults.size());

        // 1 -> 3 -> 4 -> 5 -> 6
        assertEquals("Test6", hshResults.get("Test1"));
        assertEquals("Test6", hshResults.get("Test3"));

        assertEquals("NEW_Test2", hshResults.get("Test2"));

        tmpSrcDump.delete();
        tmpTargetDump.delete();
    }

    @Test
    public void testRedirectionCycle() throws IOException, XMLStreamException {
        // source dump
        File tmpSrcDump = File.createTempFile("wiki-src-dump", "xml");
        File tmpTargetDump = File.createTempFile("wiki-target-dump", "xml");

        // base structure - "<mediawiki><page><title></title><id><id></page></mediawiki>"
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpSrcDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "</page>"
                + "<page>"
                + "<title>Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "</page>"
                + "</mediawiki>");
        bw.close();

        bw = new BufferedWriter(new FileWriter(tmpTargetDump));
        bw.write("<mediawiki>"
                + "<page>"
                + "<title>Test1</title>"
                + "<id>1</id>"
                + "<redirect/>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">#REDIRECT [[Test3]] {{R from CamelCase}}</text>"
                + "</revision>"
                + "</page>"
                + "<page>"
                + "<title>NEW_Test2</title>"
                + "<id>2</id>"
                + "</page>"
                + "<page>"
                + "<title>Test3</title>"
                + "<id>3</id>"
                + "<redirect/>"
                + "<revision>"
                + "<id>1234556</id>"
                + "<text xml:space=\"preserve\">#REDIRECT [[Test1]] {{R from CamelCase}}</text>"
                + "</revision>"
                + "</page>"
                + "</mediawiki>");
        bw.close();
        Map<String, String> hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump);
        assertEquals(3, hshResults.size());
        // Test1 REDIRECTS TO Test3, but since it forms a cycle, Test1 is mapped to itself
        assertEquals(true, !hshResults.get("Test1").equals("Test3"));

        tmpSrcDump.delete();
        tmpTargetDump.delete();
    }

    //@Ignore("score computation not implemented yet")
    @Test
    public void testDisambiguationEntries() throws IOException, XMLStreamException, URISyntaxException {
        // source dump
        URL srcUrl = getClass().getResource("/data/Einstein_source.txt");
        URL targetUrl = getClass().getResource("/data/Einstein_target.txt");

        File tmpSrcDump = new File(srcUrl.toURI());
        File tmpTargetDump = new File(targetUrl.toURI());

        // default diff will also include all unchanged entries
        Map<String, String> hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump);
        assertEquals(7, hshResults.size());

        // setting the flag to false will include unchanged entries
        hshResults = WikipediaRevisionMapper.map(tmpSrcDump, tmpTargetDump, false);
        // assertEquals(1, hshResults.size());
        assertEquals("Albert Einstein", hshResults.get("Einstein"));
    }

    @Test
    public void testWriteToXMLFile() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        File tmpFile = File.createTempFile("wikiMapperXMLTest", ".xml");
        List<MappedResult> results = new ArrayList<>();
        results.add(new MappedResult(1, "Test1", null, 1, "Test1", null, MappedType.UNCHANGED));
        results.add(new MappedResult(2, "Test2", null, 22, "Test22", null, MappedType.DISAMBIGUATED));
        results.add(new MappedResult(3, "Test3", null, 3, "Test3a", null, MappedType.UPDATED));

        String expectedContent =
                "<?xml version=\"1.0\" ?>" +
                "<MappingResult>" +
                    "<Entry>" +
                        "<MappingType>UNCHANGED</MappingType>" +
                        "<SourceId>1</SourceId>" +
                        "<SourceTitle>Test1</SourceTitle>" +
                        "<TargetId>1</TargetId>" +
                        "<TargetTitle>Test1</TargetTitle>" +
                    "</Entry>" +
                    "<Entry>" +
                        "<MappingType>DISAMBIGUATED</MappingType>" +
                        "<SourceId>2</SourceId>" +
                        "<SourceTitle>Test2</SourceTitle>" +
                        "<TargetId>22</TargetId>" +
                        "<TargetTitle>Test22</TargetTitle>" +
                    "</Entry>" +
                    "<Entry>" +
                        "<MappingType>UPDATED</MappingType>" +
                        "<SourceId>3</SourceId>" +
                        "<SourceTitle>Test3</SourceTitle>" +
                        "<TargetId>3</TargetId>" +
                        "<TargetTitle>Test3a</TargetTitle>" +
                    "</Entry>" +
                "</MappingResult>";

        Method method = WikipediaRevisionMapper.class.getDeclaredMethod("writeFileContentAsXML", File.class, List.class);
        method.setAccessible(true);
        method.invoke(null, tmpFile, results);

        String fileContent = new String(Files.readAllBytes(Paths.get(tmpFile.getAbsolutePath())), new FileReader(tmpFile).getEncoding());

        assertEquals(expectedContent, fileContent);

        tmpFile.delete();
    }
}