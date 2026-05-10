package com.example.javamate.agent.critic;

import java.util.List;

/**
 * Structured output produced by the CriticAgent.
 *
 * @param approved    true if the draft is good enough to ship
 * @param issues      short bullet strings (empty when approved)
 * @param suggestions concrete fixes the drafter should apply (empty when approved)
 */
public record CritiqueResult(
        boolean approved,
        List<String> issues,
        String suggestions
) {}
