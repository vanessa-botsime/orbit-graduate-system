package com.orbit.tools;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rules-based CV vs job description keyword match. Deliberately simple and explainable:
 * it is a guide to missing keywords, not a model of how any real ATS ranks candidates.
 * An LLM-based reviewer can sit on top of this later (see docs/TASK_BOARD.md).
 */
@Service
public class CvMatchService {

    private static final int MAX_KEYWORDS = 25;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "you", "your", "our", "are", "will", "have", "has", "this", "that",
            "from", "what", "who", "all", "can", "not", "but", "able", "about", "work", "working", "team",
            "role", "job", "experience", "years", "year", "must", "should", "strong", "good", "looking",
            "join", "also", "such", "other", "including", "into", "their", "they", "them", "more", "than",
            "well", "been", "being", "per", "etc", "any", "may", "new", "use", "using", "used", "make",
            "ability", "skills", "knowledge", "understanding", "responsibilities", "requirements", "candidate");

    public MatchResult score(String cvText, String jobDescription) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String token : tokens(jobDescription)) {
            frequency.merge(token, 1, Integer::sum);
        }
        List<String> keywords = frequency.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_KEYWORDS)
                .map(Map.Entry::getKey)
                .toList();

        Set<String> cvTokens = new HashSet<>(tokens(cvText));
        Map<Boolean, List<String>> split = keywords.stream()
                .collect(Collectors.partitioningBy(cvTokens::contains));
        List<String> matched = split.get(true);
        List<String> missing = split.get(false);

        int score = keywords.isEmpty() ? 0 : Math.round(100f * matched.size() / keywords.size());
        return new MatchResult(score, matched, missing);
    }

    static List<String> tokens(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9+#]+"))
                .filter(t -> t.length() >= 2)
                .filter(t -> !t.chars().allMatch(Character::isDigit))
                .filter(t -> !STOP_WORDS.contains(t))
                .toList();
    }
}
