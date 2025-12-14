package com.searchengine.search;

import com.searchengine.utils.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates search result snippets with keyword highlighting.
 */
public class SnippetGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SnippetGenerator.class);

    private final int snippetLength;
    private final int contextSize;

    public SnippetGenerator() {
        this.snippetLength = ConfigLoader.getInt("search.snippet.length", 200);
        this.contextSize = snippetLength / 2;
    }

    /**
     * Generate a snippet from content highlighting query terms.
     *
     * @param content    The page content
     * @param queryTerms Original query terms (not stemmed, for highlighting)
     * @return Snippet with highlighted keywords
     */
    public String generate(String content, List<String> queryTerms) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        if (queryTerms == null || queryTerms.isEmpty()) {
            return truncate(content, snippetLength);
        }

        // Find the first occurrence of any query term
        String lowerContent = content.toLowerCase();
        int bestPosition = -1;
        String matchedTerm = null;

        for (String term : queryTerms) {
            int pos = lowerContent.indexOf(term.toLowerCase());
            if (pos >= 0 && (bestPosition < 0 || pos < bestPosition)) {
                bestPosition = pos;
                matchedTerm = term;
            }
        }

        String snippet;
        if (bestPosition >= 0) {
            // Extract context around the first match
            int start = Math.max(0, bestPosition - contextSize);
            int end = Math.min(content.length(), bestPosition + matchedTerm.length() + contextSize);

            StringBuilder sb = new StringBuilder();
            if (start > 0) {
                sb.append("...");
            }
            sb.append(content.substring(start, end).trim());
            if (end < content.length()) {
                sb.append("...");
            }
            snippet = sb.toString();
        } else {
            // No match found, use beginning of content
            snippet = truncate(content, snippetLength);
        }

        // Highlight all query terms
        snippet = highlightTerms(snippet, queryTerms);

        return snippet;
    }

    /**
     * Highlight query terms in the snippet.
     */
    private String highlightTerms(String text, List<String> terms) {
        String result = text;
        for (String term : terms) {
            // Case-insensitive replacement
            Pattern pattern = Pattern.compile("(?i)(" + Pattern.quote(term) + ")");
            Matcher matcher = pattern.matcher(result);
            result = matcher.replaceAll("**$1**");
        }
        return result;
    }

    /**
     * Truncate text to specified length with ellipsis.
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        // Find a word boundary near maxLength
        int end = maxLength;
        while (end > maxLength - 20 && end > 0 && !Character.isWhitespace(text.charAt(end))) {
            end--;
        }

        if (end <= 0) {
            end = maxLength;
        }

        return text.substring(0, end).trim() + "...";
    }
}
