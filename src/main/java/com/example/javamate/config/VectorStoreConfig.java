package com.example.javamate.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.mistralai.MistralAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VectorStoreConfig {

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
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls)
                .withApiKey(qdrantApiKey);
        
        return new QdrantClient(grpcClientBuilder.build());
    }

    @Bean
    @Primary
    public VectorStore vectorStore(QdrantClient qdrantClient, MistralAiEmbeddingModel mistralAiEmbeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, mistralAiEmbeddingModel)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();
    }
}

