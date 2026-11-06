package com.orbit.events;

import com.orbit.config.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository repo;

    public AuditController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<AuditView> recent(Authentication auth) {
        return repo.findTop50ByOwnerIdOrderByOccurredAtDesc(CurrentUser.id(auth))
                .stream().map(AuditView::from).toList();
    }
}
