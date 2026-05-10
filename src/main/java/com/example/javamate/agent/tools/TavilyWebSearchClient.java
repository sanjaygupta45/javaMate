package com.example.javamate.agent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Tavily AI search (https://tavily.com) - small JSON API designed for LLM agents.
 *
 * <p>Activated when {@code javamate.web-search.provider=tavily} and an API key is provided.
 */
@Component
@ConditionalOnProperty(name = "javamate.web-search.provider", havingValue = "tavily")
public class TavilyWebSearchClient implements WebSearchClient {

    private static final Logger log = LoggerFactory.getLogger(TavilyWebSearchClient.class);
    private static final String ENDPOINT = "https://api.tavily.com/search";

    private final WebClient webClient;
    private final String apiKey;

    public TavilyWebSearchClient(@Value("${javamate.web-search.tavily.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(ENDPOINT)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Tavily provider selected but javamate.web-search.tavily.api-key is empty.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SearchResult> search(String query, int maxResults) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> body = Map.of(
                    "api_key", apiKey,
                    "query", query,
                    "max_results", maxResults,
                    "search_depth", "basic",
                    "include_answer", false
            );
            Map<String, Object> resp = webClient.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            if (resp == null) {
                return List.of();
            }
            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) resp.getOrDefault("results", List.of());
            return results.stream()
                    .map(r -> new SearchResult(
                            String.valueOf(r.getOrDefault("title", "")),
                            String.valueOf(r.getOrDefault("url", "")),
                            String.valueOf(r.getOrDefault("content", ""))))
                    .toList();
        } catch (Exception e) {
            log.error("Tavily search failed for query='{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
