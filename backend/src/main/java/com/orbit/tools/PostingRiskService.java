package com.orbit.tools;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Rules-based scam-signal checker for job postings. Offline, no external lookups yet. */
@Service
public class PostingRiskService {

    private static final Set<String> FREE_MAIL = Set.of(
            "gmail.com", "googlemail.com", "outlook.com", "hotmail.com", "yahoo.com", "icloud.com", "live.com");
    private static final List<String> PAYMENT_PHRASES = List.of(
            "registration fee", "application fee", "processing fee", "training fee", "uniform fee",
            "pay a fee", "deposit required", "pay for your");
    private static final List<String> CHAT_APPS = List.of("whatsapp", "telegram");
    private static final List<String> GUARANTEE_PHRASES = List.of(
            "guaranteed income", "guaranteed salary", "guaranteed job", "earn up to", "no experience needed");
    private static final Pattern SENSITIVE = Pattern.compile(
            "\\b(otp|one-time pin|bank(ing)? details|bank account details|card number|cvv)\\b");

    public RiskResult assess(RiskRequest r) {
        int points = 0;
        List<String> reasons = new ArrayList<>();
        String text = r.postingText() == null ? "" : r.postingText().toLowerCase(Locale.ROOT);
        String emailDomain = emailDomain(r.contactEmail());
        String siteHost = host(r.websiteUrl());

        if (emailDomain != null && FREE_MAIL.contains(emailDomain)) {
            points += 2;
            reasons.add("Contact email uses a free provider (" + emailDomain + ") instead of a company domain");
        } else if (emailDomain != null && siteHost != null && !domainsRelated(emailDomain, siteHost)) {
            points += 1;
            reasons.add("Contact email domain (" + emailDomain + ") does not match the company website (" + siteHost + ")");
        }
        if (siteHost == null) {
            points += 1;
            reasons.add("No company website was provided to check");
        }
        if (PAYMENT_PHRASES.stream().anyMatch(text::contains)) {
            points += 4;
            reasons.add("Mentions a fee or payment you would have to make");
        }
        if (SENSITIVE.matcher(text).find()) {
            points += 3;
            reasons.add("Asks for banking details, OTPs or card information");
        }
        if (CHAT_APPS.stream().anyMatch(text::contains)) {
            points += 2;
            reasons.add("Moves the conversation to WhatsApp or Telegram");
        }
        if (GUARANTEE_PHRASES.stream().anyMatch(text::contains)) {
            points += 2;
            reasons.add("Uses guaranteed-income or too-good-to-be-true wording");
        }

        String level = points >= 5 ? "HIGH" : points >= 2 ? "MEDIUM" : "LOW";
        if (reasons.isEmpty()) {
            reasons.add("No common scam signals found. Still confirm the company on its official website and CIPC.");
        }
        return new RiskResult(level, points, reasons);
    }

    private static String emailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        return email.substring(email.lastIndexOf('@') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private static String host(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String withScheme = url.contains("://") ? url.trim() : "https://" + url.trim();
            String h = URI.create(withScheme).getHost();
            return h == null ? null : h.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean domainsRelated(String emailDomain, String siteHost) {
        return emailDomain.equals(siteHost)
                || siteHost.endsWith("." + emailDomain)
                || emailDomain.endsWith("." + siteHost);
    }
}
