package com.example.javamate.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mistralai.MistralAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${spring.ai.vectorstore.qdrant.host}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port}")
    private int qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.api-key}")
    private String qdrantApiKey;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.qdrant.use-tls:true}")
    private boolean useTls;

    @Bean
    @ConditionalOnMissingBean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls)
                        .withApiKey(qdrantApiKey)
                        .build()
        );
    }

    @Bean
    @Primary
    public VectorStore vectorStore(QdrantClient qdrantClient, MistralAiEmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();
    }

    @Bean
    public CommandLineRunner initQdrantIndex(QdrantClient qdrantClient) {
        return args -> {
            try {
                qdrantClient.createPayloadIndexAsync(
                        collectionName, "userId", Collections.PayloadSchemaType.Keyword,
                        null, true, null, null
                ).get();
                log.info("Qdrant index for 'userId' created");
            } catch (Exception e) {
                log.debug("Qdrant index for 'userId' may already exist: {}", e.getMessage());
            }
        };
    }
}
