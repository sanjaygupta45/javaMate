package com.example.javamate.agent.tools;

import com.example.javamate.agent.events.AgentCallContext;
import com.example.javamate.agent.events.AgentEventBus;
import com.example.javamate.agent.tracing.AgentTracing;
import com.example.javamate.agent.events.AgentStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class WebSearchTools {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTools.class);
    private static final int MAX_RESULTS = 5;

    private final WebSearchClient client;
    private final AgentTracing tracing;
    private final AgentEventBus eventBus;

    public WebSearchTools(WebSearchClient client, AgentTracing tracing, AgentEventBus eventBus) {
        this.client = client;
        this.tracing = tracing;
        this.eventBus = eventBus;
    }

    @Tool(description = "Search the public web for up-to-date information: latest library versions, "
            + "release notes, recent CVEs, blog posts, documentation links and news. "
            + "Returns a short list of results with title, URL and snippet. "
            + "Call this whenever the user asks about something that changes over time "
            + "or that you are uncertain about.")
    public String webSearch(
            @ToolParam(description = "A focused search query in plain English") String query) {
        return tracing.span(
                "tool.web_search",
                Map.of("tool.name", "webSearch",
                        "tool.query", query == null ? "" : query),
                () -> {
                    log.info("[WebSearchTool] query='{}'", query);
                    List<WebSearchClient.SearchResult> results = client.search(query, MAX_RESULTS);
                    int count = results == null ? 0 : results.size();
                    tracing.tag("tool.results.count", count);

                    // Publish a tool event so the UI can render sources / show a "searching the web..." chip.
                    String convId = AgentCallContext.get();
                    if (convId != null) {
                        Map<String, Object> args = new LinkedHashMap<>();
                        args.put("query", query);
                        args.put("maxResults", MAX_RESULTS);
                        List<Map<String, String>> serialised = results == null ? List.of() :
                                results.stream().map(r -> {
                                    Map<String, String> m = new LinkedHashMap<>();
                                    m.put("title", r.title());
                                    m.put("url", r.url());
                                    m.put("snippet", r.snippet());
                                    return m;
                                }).toList();
                        eventBus.emit(convId,
                                new AgentStreamEvent.ToolEvent("webSearch", args, serialised));
                    }

                    if (count == 0) {
                        return "No web results found.";
                    }
                    return results.stream()
                            .map(r -> "- " + r.title() + " (" + r.url() + ")\n  " + r.snippet())
                            .collect(Collectors.joining("\n"));
                });
    }
}
