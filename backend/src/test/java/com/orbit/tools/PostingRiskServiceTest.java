package com.orbit.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostingRiskServiceTest {

    private final PostingRiskService service = new PostingRiskService();

    @Test
    void feeRequestFromFreeMailIsHighRisk() {
        RiskResult r = service.assess(new RiskRequest("Acme", "hr.acme@gmail.com", null,
                "Pay a registration fee and continue on WhatsApp", null));
        assertEquals("HIGH", r.level());
        assertFalse(r.reasons().isEmpty());
    }

    @Test
    void matchingCompanyDomainIsLowRisk() {
        RiskResult r = service.assess(new RiskRequest("Acme", "careers@acme.co.za", "https://www.acme.co.za",
                "Graduate developer programme, apply via our portal.", null));
        assertEquals("LOW", r.level());
    }

    @Test
    void mismatchedDomainAddsRisk() {
        RiskResult r = service.assess(new RiskRequest("Acme", "jobs@other-mail.net", "https://acme.co.za",
                "Graduate programme", null));
        assertEquals(1, r.points());
        assertTrue(r.reasons().get(0).contains("does not match"));
    }
}
