package com.orbit.tools;

import java.util.List;

public record MatchResult(int score, List<String> matched, List<String> missing) {
}
