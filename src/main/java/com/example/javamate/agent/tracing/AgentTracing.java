package com.example.javamate.agent.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Thin facade over Micrometer Tracing so every agent / tool emits a span without
 * each one having to know about the Tracer API.
 *
 * <p>Spans are visible in Grafana Tempo via the OTLP exporter already configured
 * in {@code application.properties}.
 *
 * <p>If tracing is not configured (e.g. in a unit test), all methods become no-ops.
 */
@Component
public class AgentTracing {

    private final ObjectProvider<Tracer> tracerProvider;

    public AgentTracing(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    /** Run a blocking unit of work inside a span. */
    public <T> T span(String name, Map<String, String> tags, Supplier<T> work) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            return work.get();
        }
        Span span = tracer.nextSpan().name(name);
        if (tags != null) tags.forEach(span::tag);
        span.start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return work.get();
        } catch (RuntimeException e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /** Tag the current span (no-op if none active). */
    public void tag(String key, String value) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) return;
        Span s = tracer.currentSpan();
        if (s != null && value != null) s.tag(key, value);
    }

    public void tag(String key, long value) {
        tag(key, String.valueOf(value));
    }

    /** Wrap a Mono so a span lives for its full lifecycle. */
    public <T> Mono<T> wrap(String name, Map<String, String> tags, Mono<T> mono) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) return mono;
        return Mono.using(
                () -> {
                    Span s = tracer.nextSpan().name(name);
                    if (tags != null) tags.forEach(s::tag);
                    return s.start();
                },
                span -> mono.doOnError(span::error),
                Span::end
        );
    }

    /** Wrap a Flux so a span lives for its full lifecycle and counts emitted chunks. */
    public <T> Flux<T> wrap(String name, Map<String, String> tags, Flux<T> flux) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) return flux;
        return Flux.using(
                () -> {
                    Span s = tracer.nextSpan().name(name);
                    if (tags != null) tags.forEach(s::tag);
                    return s.start();
                },
                span -> {
                    AtomicLong chars = new AtomicLong();
                    return flux
                            .doOnNext(t -> { if (t != null) chars.addAndGet(t.toString().length()); })
                            .doOnError(span::error)
                            .doOnTerminate(() -> span.tag("agent.output.chars", String.valueOf(chars.get())));
                },
                Span::end
        );
    }
}
