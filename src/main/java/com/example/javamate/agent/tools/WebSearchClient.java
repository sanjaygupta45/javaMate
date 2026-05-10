package com.example.javamate.agent.tools;

import java.util.List;

/**
 * Pluggable abstraction over a web-search provider (Tavily, Brave, SerpAPI, ...).
 * Implementations must be safe to call from a blocking thread (boundedElastic).
 */
public interface WebSearchClient {

    List<SearchResult> search(String query, int maxResults);

    record SearchResult(String title, String url, String snippet) {}
}
