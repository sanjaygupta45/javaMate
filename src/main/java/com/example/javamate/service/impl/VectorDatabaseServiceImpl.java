package com.example.javamate.service.impl;

import com.example.javamate.service.VectorDatabaseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorDatabaseServiceImpl implements VectorDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(VectorDatabaseServiceImpl.class);

    private static final int BATCH_SIZE = 10;
    private static final long DELAY_BETWEEN_BATCHES_MS = 2000;
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 5000;

    private final VectorStore vectorStore;

    @Override
    public void storeBatch(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        int totalDocs = documents.size();
        int totalBatches = (int) Math.ceil((double) totalDocs / BATCH_SIZE);

        log.info("Storing {} documents in {} batches (batch size: {})", totalDocs, totalBatches, BATCH_SIZE);

        for (int i = 0; i < totalDocs; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalDocs);
            List<Document> batch = documents.subList(i, end);
            int batchNumber = (i / BATCH_SIZE) + 1;

            storeBatchWithRetry(batch, batchNumber, totalBatches);

            // Add delay between batches to respect rate limits (except after last batch)
            if (end < totalDocs) {
                sleep(DELAY_BETWEEN_BATCHES_MS);
            }
        }

        log.info("Successfully stored all {} documents", totalDocs);
    }

    private void storeBatchWithRetry(List<Document> batch, int batchNumber, int totalBatches) {
        int attempt = 0;
        long retryDelay = INITIAL_RETRY_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                log.info("Processing batch {}/{} with {} documents (attempt {})",
                        batchNumber, totalBatches, batch.size(), attempt + 1);
                vectorStore.add(batch);
                log.info("Batch {}/{} completed successfully", batchNumber, totalBatches);
                return; // Success - exit retry loop
            } catch (Exception e) {
                attempt++;
                String errorMessage = e.getMessage();
                
                // Check if it's a rate limit error (429)
                if (errorMessage != null && errorMessage.contains("429")) {
                    if (attempt < MAX_RETRIES) {
                        log.warn("Rate limit hit for batch {}/{}. Waiting {} seconds before retry {}/{}",
                                batchNumber, totalBatches, retryDelay / 1000, attempt, MAX_RETRIES);
                        sleep(retryDelay);
                        retryDelay *= 2; // Exponential backoff
                    } else {
                        log.error("Max retries exceeded for batch {}/{}. Last error: {}", 
                                batchNumber, totalBatches, errorMessage);
                        throw e;
                    }
                }
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Batch processing interrupted", e);
        }
    }


    @Override
    public List<Document> similaritySearchByUserId(String query, int topK, Long userId) {

        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filterExpression = builder.eq("userId", userId.toString()).build();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filterExpression)
                .build();

        return vectorStore.similaritySearch(request);
    }
}