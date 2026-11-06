package com.orbit.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CvMatchServiceTest {

    private final CvMatchService service = new CvMatchService();

    @Test
    void reportsMatchedAndMissingKeywords() {
        MatchResult r = service.score("Built REST APIs with Java and Spring", "Java Spring Kafka PostgreSQL developer");
        assertTrue(r.matched().contains("java"));
        assertTrue(r.matched().contains("spring"));
        assertTrue(r.missing().contains("kafka"));
        assertTrue(r.score() > 0 && r.score() < 100);
    }

    @Test
    void fullOverlapScoresHundred() {
        MatchResult r = service.score("java spring kafka", "Java, Spring and Kafka");
        assertEquals(100, r.score());
        assertTrue(r.missing().isEmpty());
    }

    @Test
    void stopWordsAndNumbersAreIgnored() {
        MatchResult r = service.score("anything", "the and 2024 with");
        assertEquals(0, r.score());
        assertTrue(r.matched().isEmpty() && r.missing().isEmpty());
    }

    @Test
    void keepsLanguageSymbols() {
        MatchResult r = service.score("I use C# daily", "C# developer");
        assertTrue(r.matched().contains("c#"));
    }
}
