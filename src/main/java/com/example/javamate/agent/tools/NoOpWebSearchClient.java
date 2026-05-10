package com.example.javamate.agent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fallback web-search client used when no real provider is configured.
 * Lets the application start even without a web-search API key.
 */
@Component
@ConditionalOnMissingBean(value = WebSearchClient.class, ignored = NoOpWebSearchClient.class)
public class NoOpWebSearchClient implements WebSearchClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpWebSearchClient.class);

    public NoOpWebSearchClient() {
        log.warn("No web-search provider configured. Set javamate.web-search.provider=tavily "
                + "and javamate.web-search.tavily.api-key to enable real web search.");
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        return List.of(new SearchResult(
                "Web search disabled",
                "about:blank",
                "Web search is not configured on this instance. "
                        + "Ask the operator to set javamate.web-search.tavily.api-key."
        ));
    }
}
