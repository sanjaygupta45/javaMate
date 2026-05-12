package com.example.javamate.agent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Agent {
    AgentName name();
    Mono<AgentResult> run(AgentContext ctx);
    default Flux<String> stream(AgentContext ctx) {
        return run(ctx).flatMapMany(r -> Flux.just(r.content()));
    }
}
