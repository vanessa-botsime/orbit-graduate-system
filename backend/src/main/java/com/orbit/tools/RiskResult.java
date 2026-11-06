package com.orbit.tools;

import java.util.List;

/** level is LOW, MEDIUM or HIGH. This is a risk indicator, never proof that a company is real or fake. */
public record RiskResult(String level, int points, List<String> reasons) {
}
