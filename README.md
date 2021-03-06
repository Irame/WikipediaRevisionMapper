# Wikipedia Revision Mapper

Wikipedia URLs change over time and are thus not a good choice as stable identifier. This is especially problematic when Wikipedia URLs are used as IDs, e.g. in systems that link ambiguous names to Wikipedia. This tool provides a mapping between two Wikipedia dumps from different points in time.

## Reasons for URL Changes

There are many reason for URL changes:
 * Guidelines change: Wikipedia editors jointly decide on a naming scheme that enforces some consistency (http://en.wikipedia.org/wiki/Wikipedia:Proposed_naming_conventions_and_guidelines) - usually the page id stays and the original name becomes a redirect.
 * New entities with existing names appear: entity pages become disambiguation pages, the actual entities get a "()" qualifier attached.
 * Editors decide on a per-article basis that the name is not a good fit.
 
Often, the page IDs stay, and can be used as the main feature to decide the equivalence between old and new urls. However, this is not always the case.

## Mapping URLs

This tool uses two single Wikipedia dumps of the appropriate timestamps [1] to create the mappings, where the dumps are of the style
"Recombine articles, templates, media/file descriptions, and primary meta-pages.", named ...-pages-articles.xml. These dumps can be downloaded
from https://dumps.wikimedia.org/backup-index.html.

Input:
 * Two Wikipedia pages-articles.xml dumps at different points in time
 	* Source dump - earlier dump, providing the URLs of the source.
 	* Target dump - more recent dump, providing the URLs of the mapping target. 
Output:
 * List of new URLs corresponding to the old URLs.
 	* Each entry has source URL and corresponding target URL along with mapping information

## Mapping Features

The following are the criteria for mapping source urls to target urls:
 * If the target page is a redirect and the source page isn't, the source url is mapped to the last wiki page in the redirection chain.
 * If the target page is a disambiguation and the source page isn't, outgoing links of source page are compared with outgoing links of each of the disambiguation choices. The target page with maximum similarity measure (using Jaccard overlap over the linked articles) is mapped to the source url.
 * A source entry that has been deleted in the target dump will be mapped to "".
 * A source url, that has same id as the target page but a different target title, will be mapped to updated title.
 * If none of the above conditions are satisfied, then the source url is unchanged in the target dump.

## Examples

Input:
 * Source Dump: enwiki-20100817-pages-articles.xml
 * Target Dump: enwiki-20140811-pages-articles.xml

Output:
 * A tab-separated UTF-8 text file where each line corresponds to one mapping with additional information:
 	
 SOURCE-URL	TARGET-URL	MAP-TYPE
 	
 where MAP_TYPE can be:
 - REDIRECTED - The target URL is obtained via redirect page
 - REDIRECTED_CYCLE - The target URL redirects to itself or to entry that redirects back to the URL.
 - DISAMBIGUATED - The mapping is obtained by computing the similarity of source URL with all disambiguate pages the target points to.
 - REDIRECTED_DISAMBIGUATED - The source page is a redirect page and the target of this redirection is used for the similarity measurement.
 - REDIRECTED_CYCLE_DISAMBIGUATED - The source URL redirects to itself or to entry that redirects back to the URL and the target page is a disambiguation page.
 - UNCHANGED - Source page is same as target.
 - UPDATED - Source page id is same as target id but title information has been changed
 - DELETED - The page entry in source dump has been removed in the target.

Sample:
	
	... (other mappings) ...
	http://en.wikipedia.org/wiki/People%27s_Republic_of_China	http://en.wikipedia.org/wiki/China	REDIRECTED
	...

### Evaluation Mode Output

In evaluation mode the output file is a XML file which holds more detailed information then the normal output:

```
MappingResult
  ↳ Entry
      ↳ MappingType
      ↳ SourceId
      ↳ SourceTitle
     [↳ SourceText]
      ↳ TargetId
      ↳ TargetTitle
     [↳ TargetText]
```
`MappingResult` is the root and there is one `Entry` for each mapping.
`SourceText` and `TargetText` are only there if the target page is a disambiguation and the source page isn't.

## Usage

Compile the project using:

```
mvn compile
```

Run the mapping process:

```
./scripts/run.sh -s <OLD_DUMP_FILE_PATH> -t <NEW_DUMP_FILE_PATH> -w <MAPPING_FILE> [-e]
```

## Requirements

 * Java 8
 * 12GB of main memory. For the evaluation mode, the main memory required increases due to additional text storage for comparison.

## Quality

To estimate how well the disambiguation heuristic works, we evaluated it in the following setup.

1. Around 1000 disambiguation entries are randomly selected from the result and are verified manually by comparing the source text and target text.
2. Correctness value was finally computed based on the number of correctly mapped disambiguation entries using the Wilson Coefficient. (Some mappings whose source url itself is disambiguation page which were not filtered due use of old marker texts are ignored while computing the correctness)
3. The correctness lies currently at 81.29%.

## Authors

* Johannes Hoffart
* Felix Keller
* Vasanth Venkatraman