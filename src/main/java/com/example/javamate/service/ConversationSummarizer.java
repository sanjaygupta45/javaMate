package com.example.javamate.service;

import com.example.javamate.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Component
public class ConversationSummarizer {

    private static final Logger log = LoggerFactory.getLogger(ConversationSummarizer.class);

    private static final int MAX_TRANSCRIPT_CHARS = 16_000;

    private static final String SYSTEM_PROMPT = """
            You are a "handoff" summariser. The user has reached the message limit
            on one chat session and is starting a fresh one. Your job is to give the
            *next* assistant everything it needs to feel like a seamless continuation.

            Produce a concise summary (150-300 words) using this structure exactly:

            1. **Topic** - one line: what the user is working on.
            2. **Key facts & decisions** - 3-7 bullets of concrete information already
               established (versions chosen, files touched, design choices made).
            3. **Code / artefacts** - any class names, method signatures, libraries
               or commands that came up. Keep this terse.
            4. **Open questions** - what was unresolved or what the user was about
               to ask next.
            5. **User preferences** - tone, language, style hints the assistant
               picked up (e.g. "wants Spring Boot 4 examples", "uses Maven").

            Rules:
              - Be factual; never invent details.
              - No greetings, no apologies, no "in this conversation we...".
              - Plain markdown, no code fences around the whole answer.
              - If a section has nothing to record, write "_(none)_".
            """;

    private final ChatClient chat;

    public ConversationSummarizer(MistralAiChatModel model) {
        this.chat = ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .build();
    }

    /**
     * Summarise the given (ordered) transcript. Runs the blocking LLM call on the
     * elastic scheduler so callers stay reactive.
     */
    public Mono<String> summarise(List<ChatMessage> orderedMessages) {
        if (orderedMessages == null || orderedMessages.isEmpty()) {
            return Mono.just("_(no prior conversation)_");
        }
        String transcript = buildTranscript(orderedMessages);
        return Mono.fromCallable(() -> chat.prompt().user(transcript).call().content())
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(s -> log.info("Generated handoff summary ({} chars)", s == null ? 0 : s.length()))
                .doOnError(e -> log.error("Handoff summariser failed: {}", e.getMessage()))
                .onErrorReturn("_(summary unavailable - LLM error)_");
    }

    private static String buildTranscript(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder("Transcript to summarise (most recent last):\n\n");
        for (ChatMessage m : messages) {
            String role = m.getRole() == null ? "?" : m.getRole();
            String content = m.getContent() == null ? "" : m.getContent();
            sb.append(role).append(": ").append(content).append("\n\n");
            if (sb.length() > MAX_TRANSCRIPT_CHARS) {
                sb.append("[... transcript truncated ...]\n");
                break;
            }
        }
        return sb.toString();
    }
}

