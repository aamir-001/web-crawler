package com.searchengine;

import com.searchengine.database.DatabaseManager;
import com.searchengine.database.Page;
import com.searchengine.database.PageDAO;
import com.searchengine.indexer.Indexer;
import com.searchengine.search.QueryProcessor;
import com.searchengine.search.SearchEngine;
import com.searchengine.search.SearchResult;
import com.searchengine.search.SnippetGenerator;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phase 4 Search Engine components.
 */
public class SearchEngineTest {

    private static DatabaseManager dbManager;
    private static PageDAO pageDAO;
    private static Indexer indexer;
    private static SearchEngine searchEngine;
    private static final String TEST_DB = "data/test_search.db";

    @BeforeAll
    public static void setUp() throws Exception {
        new File("data").mkdirs();
        File testDb = new File(TEST_DB);
        if (testDb.exists()) {
            testDb.delete();
        }

        dbManager = DatabaseManager.getInstance(TEST_DB, 5);
        pageDAO = new PageDAO(dbManager);

        // Insert test pages
        Page page1 = new Page("https://example.com/java", "Java Programming",
                "Java is a popular programming language used for web development and enterprise applications.", 0);
        Page page2 = new Page("https://example.com/python", "Python Programming",
                "Python is a versatile programming language known for its simplicity.", 0);
        Page page3 = new Page("https://example.com/java-tips", "Java Tips and Tricks",
                "Learn advanced Java programming techniques and best practices for Java developers.", 0);

        pageDAO.insertPage(page1);
        pageDAO.insertPage(page2);
        pageDAO.insertPage(page3);

        // Index pages
        indexer = new Indexer(dbManager);
        indexer.indexAllPages();

        // Create search engine
        searchEngine = new SearchEngine(dbManager, indexer);
    }

    @AfterAll
    public static void tearDown() {
        if (dbManager != null) {
            dbManager.shutdown();
        }
        new File(TEST_DB).delete();
    }

    @Test
    @DisplayName("QueryProcessor: Process search query")
    public void testQueryProcessor() {
        QueryProcessor processor = new QueryProcessor();

        List<String> terms = processor.process("Java programming");
        assertFalse(terms.isEmpty(), "Should have processed terms");
        // Check that terms are stemmed (programming -> program)
        assertTrue(terms.stream().anyMatch(t -> t.startsWith("program")),
                "Should contain stemmed 'program'");
        assertEquals(2, terms.size(), "Should have 2 terms after removing stop words");
    }

    @Test
    @DisplayName("QueryProcessor: Filter stop words")
    public void testQueryProcessorStopWords() {
        QueryProcessor processor = new QueryProcessor();

        List<String> terms = processor.process("the java and python");
        assertFalse(terms.contains("the"), "Should not contain stop word 'the'");
        assertFalse(terms.contains("and"), "Should not contain stop word 'and'");
    }

    @Test
    @DisplayName("SnippetGenerator: Generate snippet")
    public void testSnippetGenerator() {
        SnippetGenerator generator = new SnippetGenerator();

        String content = "Java is a popular programming language used worldwide.";
        String snippet = generator.generate(content, List.of("java"));

        assertNotNull(snippet, "Snippet should not be null");
        assertTrue(snippet.toLowerCase().contains("java"), "Snippet should contain keyword");
    }

    @Test
    @DisplayName("SearchEngine: Search for single term")
    public void testSearchSingleTerm() {
        List<SearchResult> results = searchEngine.search("java");

        assertFalse(results.isEmpty(), "Should find results for 'java'");
        assertTrue(results.size() >= 2, "Should find at least 2 Java pages");

        // Results should be ranked
        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).getRank(), "Rank should be correct");
        }
    }

    @Test
    @DisplayName("SearchEngine: Search for multiple terms")
    public void testSearchMultipleTerms() {
        List<SearchResult> results = searchEngine.search("java programming");

        assertFalse(results.isEmpty(), "Should find results for 'java programming'");

        // All results should have non-zero scores
        for (SearchResult result : results) {
            assertTrue(result.getScore() > 0, "Score should be positive");
        }
    }

    @Test
    @DisplayName("SearchEngine: Empty query returns empty results")
    public void testEmptyQuery() {
        List<SearchResult> results = searchEngine.search("");
        assertTrue(results.isEmpty(), "Empty query should return empty results");

        results = searchEngine.search(null);
        assertTrue(results.isEmpty(), "Null query should return empty results");
    }

    @Test
    @DisplayName("SearchEngine: Results have snippets")
    public void testResultsHaveSnippets() {
        List<SearchResult> results = searchEngine.search("programming");

        assertFalse(results.isEmpty(), "Should find results");
        for (SearchResult result : results) {
            assertNotNull(result.getSnippet(), "Result should have a snippet");
        }
    }

    @Test
    @DisplayName("SearchEngine: TF-IDF ranking")
    public void testTFIDFRanking() {
        List<SearchResult> results = searchEngine.search("java");

        // The page with more "java" occurrences should rank higher
        if (results.size() >= 2) {
            assertTrue(results.get(0).getScore() >= results.get(1).getScore(),
                    "First result should have higher or equal score");
        }
    }
}
