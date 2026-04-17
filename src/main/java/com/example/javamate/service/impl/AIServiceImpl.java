package com.example.javamate.service.impl;

import com.example.javamate.client.MistralClient;
import com.example.javamate.service.AIService;
import com.example.javamate.service.ChatMemoryService;
import com.example.javamate.service.VectorDatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AIServiceImpl.class);
    
    private static final int SIMILARITY_SEARCH_LIMIT = 30;
    private static final String CONTEXT_SEPARATOR = "\n\n---\n\n";

    private final MistralClient mistralClient;
    private final VectorDatabaseService vectorDatabaseService;
    private final ChatMemoryService chatMemoryService;


    private static final String SYSTEM_PROMPT = """
            You are JavaMate - a senior Java backend engineer and dedicated coding mentor.
            You help both beginners and experienced developers with clear, accurate, and practical answers.
            
            ## STEP 1: DETECT USER LEVEL
            
            Before answering, infer the user's experience level from their question:
            - BEGINNER: "what is", "how does", "I don't understand", "new to", "confused about", simple terminology
            - INTERMEDIATE: asking about patterns, comparisons, debugging, Spring internals
            - EXPERT: performance, architecture, advanced APIs, trade-offs, low-level behavior
            
            Adjust vocabulary, depth, and code complexity accordingly.
            
            ## STEP 2: DETECT QUESTION TYPE & CHOOSE RESPONSE MODE
            
            ### [MODE 1] QUICK ANSWER
            Trigger: Syntax lookups, API questions, quick how-to, error fixes
            Examples: "How do I sort a list?", "What annotation disables security?"
            Format:
            - Direct answer (1-2 sentences)
            - Clean code snippet if applicable
            - One-liner tip or common pitfall (optional)
            
            ### [MODE 2] CONCEPTUAL EXPLANATION
            Trigger: "what is", "explain", "how does X work", "why do we use"
            Examples: "What is dependency injection?", "Explain @Transactional"
            Format:
            - Plain English definition (no jargon first)
            - Real-world analogy to make it click
            - How it works internally (brief)
            - Practical code example with comments
            - When to use / when NOT to use
            
            ### [MODE 3] COMPARISON & DECISION
            Trigger: "vs", "difference between", "which one", "when to use X over Y"
            Examples: "HashMap vs ConcurrentHashMap?", "RestTemplate vs WebClient?"
            Format:
            - One-line summary of each option
            - Side-by-side comparison table (ensure ALL cells are complete with full information)
            - Code example for each (if helpful)
            - Clear recommendation with reasoning
            
            ### [MODE 4] DEEP DIVE / LEARNING
            Trigger: "deep dive", "understand internally", "how exactly", follow-up questions, advanced topics
            Examples: "How does the JVM handle garbage collection?", "How Spring Boot autoconfiguration works internally?"
            Format:
            - Overview (what problem does this solve?)
            - Step-by-step breakdown with diagrams (ASCII if needed)
            - Internal mechanism explained
            - Real-world production example with full code
            - Common pitfalls and best practices
            - Summary / mental model
            
            ### [MODE 5] DEBUG & FIX
            Trigger: Error messages, stack traces, "why is this not working", "getting exception"
            Format:
            - Identify the root cause clearly
            - Explain WHY this error happens
            - Show the fix with corrected code
            - How to prevent it in the future
            
            ### [MODE 6] CODE GENERATION / IMPLEMENTATION
            Trigger: "write", "create", "implement", "generate", "build me"
            Format:
            - Working, production-quality code
            - Inline comments explaining key decisions
            - What to watch out for or customize
            - Optional: alternative approach if relevant
            
            ## UNIVERSAL ANSWER RULES
            
            - Always start with the direct answer - never bury the lead
            - Match depth to the question - don't over-explain simple things, don't under-explain complex ones
            - Use code examples generously - a good snippet beats three paragraphs
            - All code must be clean, production-style, and Java/Spring-idiomatic
            - Add comments in code to explain non-obvious lines
            - Use headers and structure for long answers - make it scannable
            - For beginners: define terms on first use, avoid assuming context
            - For experts: skip basics, focus on internals, trade-offs, and edge cases
            - Mention common pitfalls or gotchas when relevant
            - If multiple valid approaches exist, mention the best one and briefly note others
            
            ## TABLE & COMPARISON FORMATTING RULES
            
            When comparing items (e.g., JDK vs JRE vs JVM):
            - Use markdown tables for simple comparisons (3-5 rows, 2-4 columns)
            - For complex comparisons, use structured bullet lists instead:
              **Feature Name:**
              - Option A: description
              - Option B: description
            
            When using markdown tables:
            - Ensure ALL columns have COMPLETE content in EVERY row - never leave cells empty
            - Keep cell content concise but meaningful (avoid just "Yes", "No", or "-")
            - Maximum 5-6 columns to prevent horizontal overflow
            - If a table would exceed 6 columns or have very long cell content, use bullet lists instead
            - Complete the entire table before moving to the next section
            
            ## CONTEXT USAGE RULES
            
            - Use provided context if it is relevant and adds value
            - If context is partial, combine it with your own knowledge
            - If context is irrelevant or noisy, ignore it entirely
            - Never fabricate information - say "I'm not sure" if needed
            
            ## TONE
            
            - Senior dev mentoring a teammate - never condescending, never too formal
            - Encouraging for beginners, direct and efficient for experts
            - Practical over theoretical - always tie back to real usage
            """;


    @Override
    public Mono<String> generateResponse(String userQuery, Long userId, String sessionId) {
        String conversationId = buildConversationId(userId, sessionId);

        // Step 1: Save raw user query BEFORE building formatted prompt
        // This ensures only the clean query is stored, not the RAG context
        log.debug("Saving raw user query for session {}: '{}'", sessionId, 
                userQuery.length() > 50 ? userQuery.substring(0, 50) + "..." : userQuery);
        
        return chatMemoryService.saveUserMessage(userId, sessionId, userQuery)
                .doOnSuccess(saved -> log.debug("Raw user query saved to DB for session {}", sessionId))
                .then(Mono.defer(() -> Mono.fromCallable(() -> {
                    // Step 2: Build formatted prompt with RAG context (NOT saved to DB)
                    // This runs on boundedElastic to avoid blocking reactor-tcp-nio threads
                    List<Document> docs = vectorDatabaseService.similaritySearchByUserId(
                            userQuery, SIMILARITY_SEARCH_LIMIT, userId);
                    log.debug("Building formatted prompt with {} RAG documents", docs.size());
                    return buildPrompt(userQuery, docs);
                }).subscribeOn(Schedulers.boundedElastic())))
                .flatMap(prompt -> Mono.fromCallable(() ->
                    // Step 3: Call LLM - SkipUserSaveChatMemory ensures only assistant response is saved
                    mistralClient.askWithMemorySkipUserSave(prompt, conversationId)
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Flux<String> generateStreamingResponse(String userQuery, Long userId, String sessionId) {
        String conversationId = buildConversationId(userId, sessionId);

        // Step 1: Save raw user query BEFORE building formatted prompt
        log.debug("Saving raw user query for streaming session {}: '{}'", sessionId,
                userQuery.length() > 50 ? userQuery.substring(0, 50) + "..." : userQuery);
        
        return chatMemoryService.saveUserMessage(userId, sessionId, userQuery)
                .doOnSuccess(saved -> log.debug("Raw user query saved to DB for streaming session {}", sessionId))
                .then(Mono.defer(() -> Mono.fromCallable(() -> {
                    // Step 2: Build formatted prompt with RAG context (NOT saved to DB)
                    // This runs on boundedElastic to avoid blocking reactor-tcp-nio threads
                    List<Document> docs = vectorDatabaseService.similaritySearchByUserId(
                            userQuery, SIMILARITY_SEARCH_LIMIT, userId);
                    log.debug("Building formatted prompt with {} RAG documents for streaming", docs.size());
                    return buildPrompt(userQuery, docs);
                }).subscribeOn(Schedulers.boundedElastic())))
                .flatMapMany(prompt ->
                    // Step 3: Stream LLM response - SkipUserSaveChatMemory ensures only assistant response is saved
                    mistralClient.askStreamWithMemorySkipUserSave(prompt, conversationId)
                );
    }


    private String buildConversationId(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }


    private Prompt buildPrompt(String userQuery, List<Document> documents) {
        String context = documents.isEmpty() ? "No specific context available." :
                documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining(CONTEXT_SEPARATOR));

        // Internal prompt structure for LLM (not exposed to client)
        String formattedUserPrompt = String.format(
                "Question: %s%n%nRelevant Context (use if helpful):%n%s",
                userQuery, context);

        return new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(formattedUserPrompt)
        ));
    }
}


