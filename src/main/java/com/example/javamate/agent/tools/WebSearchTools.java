package com.example.javamate.agent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exposes web search as an LLM-callable tool. Spring AI scans the @Tool annotated method
 * and registers it on the ChatClient via .defaultTools(...).
 */
@Component
public class WebSearchTools {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTools.class);
    private static final int MAX_RESULTS = 5;

    private final WebSearchClient client;

    public WebSearchTools(WebSearchClient client) {
        this.client = client;
    }

    @Tool(description = "Search the public web for up-to-date information: latest library versions, "
            + "release notes, recent CVEs, blog posts, documentation links and news. "
            + "Returns a short list of results with title, URL and snippet. "
            + "Call this whenever the user asks about something that changes over time "
            + "or that you are uncertain about.")
    public String webSearch(
            @ToolParam(description = "A focused search query in plain English") String query) {
        log.info("[WebSearchTool] query='{}'", query);
        List<WebSearchClient.SearchResult> results = client.search(query, MAX_RESULTS);
        if (results == null || results.isEmpty()) {
            return "No web results found.";
        }
        return results.stream()
                .map(r -> "- " + r.title() + " (" + r.url() + ")\n  " + r.snippet())
                .collect(Collectors.joining("\n"));
    }
}
