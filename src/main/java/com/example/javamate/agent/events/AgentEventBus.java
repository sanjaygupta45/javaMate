package com.example.javamate.agent.events;

import com.example.javamate.dto.stream.AgentStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class AgentEventBus {

    private static final Logger log = LoggerFactory.getLogger(AgentEventBus.class);
    private static final int REPLAY_BUFFER = 256;

    private final Map<String, Sinks.Many<AgentStreamEvent>> sinks = new ConcurrentHashMap<>();

    /** Mirror of every emitted event so non-streaming consumers can read synchronously. */
    private final Map<String, Queue<AgentStreamEvent>> snapshots = new ConcurrentHashMap<>();


    public Sinks.Many<AgentStreamEvent> register(String conversationId) {
        Sinks.Many<AgentStreamEvent> sink = Sinks.many().replay().limit(REPLAY_BUFFER);
        sinks.put(conversationId, sink);
        snapshots.put(conversationId, new ConcurrentLinkedQueue<>());
        return sink;
    }


    public Flux<AgentStreamEvent> stream(String conversationId) {
        Sinks.Many<AgentStreamEvent> s = sinks.get(conversationId);
        return s == null ? Flux.empty() : s.asFlux();
    }

    public List<AgentStreamEvent> snapshot(String conversationId) {
        Queue<AgentStreamEvent> q = snapshots.get(conversationId);
        return q == null ? List.of() : List.copyOf(q);
    }


    public void emit(String conversationId, AgentStreamEvent event) {
        if (conversationId == null || event == null) return;
        Queue<AgentStreamEvent> q = snapshots.get(conversationId);
        if (q != null) q.add(event);
        Sinks.Many<AgentStreamEvent> s = sinks.get(conversationId);
        if (s == null) return;
        Sinks.EmitResult result = s.tryEmitNext(event);
        if (result.isFailure()) {
            log.debug("[AgentEventBus] dropped event type={} for conv={} result={}",
                    event.type(), conversationId, result);
        }
    }


    public void complete(String conversationId) {
        Sinks.Many<AgentStreamEvent> s = sinks.remove(conversationId);
        snapshots.remove(conversationId);
        if (s != null) {
            s.tryEmitComplete();
        }
    }
}
