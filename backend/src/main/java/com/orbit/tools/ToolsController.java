package com.orbit.tools;

import com.orbit.application.ApplicationService;
import com.orbit.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    private final CvMatchService matcher;
    private final PostingRiskService risk;
    private final ApplicationService applications;

    public ToolsController(CvMatchService matcher, PostingRiskService risk, ApplicationService applications) {
        this.matcher = matcher;
        this.risk = risk;
        this.applications = applications;
    }

    @PostMapping("/match")
    public MatchResult match(Authentication auth, @Valid @RequestBody MatchRequest req) {
        MatchResult result = matcher.score(req.cvText(), req.jobDescription());
        if (req.applicationId() != null) {
            applications.recordMatch(CurrentUser.id(auth), req.applicationId(), result.score());
        }
        return result;
    }

    @PostMapping("/risk")
    public RiskResult risk(Authentication auth, @Valid @RequestBody RiskRequest req) {
        RiskResult result = risk.assess(req);
        if (req.applicationId() != null) {
            applications.recordRisk(CurrentUser.id(auth), req.applicationId(), result.level());
        }
        return result;
    }
}
