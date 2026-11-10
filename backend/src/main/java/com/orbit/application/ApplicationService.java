package com.orbit.application;

import com.orbit.events.ApplicationEvent;
import com.orbit.events.EventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobApplicationRepository repo;
    private final EventPublisher events;

    public ApplicationService(JobApplicationRepository repo, EventPublisher events) {
        this.repo = repo;
        this.events = events;
    }

    @Transactional
    public ApplicationResponse create(UUID ownerId, ApplicationRequest req) {
        JobApplication application = new JobApplication(ownerId);
        apply(application, req);
        JobApplication saved = repo.save(application);
        events.publish(ApplicationEvent.of("APPLICATION_CREATED", ownerId, saved.getId(), label(saved)));
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(UUID ownerId) {
        return repo.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(ApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID ownerId, UUID id) {
        return ApplicationResponse.from(find(ownerId, id));
    }

    @Transactional
    public ApplicationResponse update(UUID ownerId, UUID id, ApplicationRequest req) {
        JobApplication a = find(ownerId, id);
        ApplicationStatus before = a.getStatus();
        apply(a, req);
        a.touch();
        repo.save(a);
        if (before != a.getStatus()) {
            events.publish(ApplicationEvent.of("STATUS_CHANGED", ownerId, a.getId(),
                    label(a) + ": " + before + " -> " + a.getStatus()));
        } else {
            events.publish(ApplicationEvent.of("APPLICATION_UPDATED", ownerId, a.getId(), label(a)));
        }
        return ApplicationResponse.from(a);
    }

    @Transactional
    public void delete(UUID ownerId, @NonNull UUID id) {
        JobApplication application = find(ownerId, id);

        repo.deleteById(id);

        events.publish(ApplicationEvent.of(
            "APPLICATION_DELETED",
            ownerId,
            id,
            label(application)
        ));
    }

    /** Applications still unsent (wishlist/drafting) with a deadline in the next {@code days} days. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> dueSoon(UUID ownerId, int days) {
        LocalDate today = LocalDate.now();
        return repo.findByOwnerIdAndDeadlineBetweenAndStatusInOrderByDeadlineAsc(
                        ownerId, today, today.plusDays(days),
                        List.of(ApplicationStatus.WISHLIST, ApplicationStatus.DRAFTING))
                .stream().map(ApplicationResponse::from).toList();
    }

    @Transactional
    public void recordMatch(UUID ownerId, UUID id, int score) {
        JobApplication a = find(ownerId, id);
        a.setMatchScore(score);
        a.touch();
        events.publish(ApplicationEvent.of("MATCH_SCORED", ownerId, id, label(a) + ": " + score + "%"));
    }

    @Transactional
    public void recordRisk(UUID ownerId, UUID id, String level) {
        JobApplication a = find(ownerId, id);
        a.setRiskLevel(level);
        a.touch();
        events.publish(ApplicationEvent.of("RISK_CHECKED", ownerId, id, label(a) + ": " + level));
    }

    private JobApplication find(UUID ownerId, UUID id) {
        // Scoping by owner means one user can never read or change another user's data.
        return repo.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private void apply(JobApplication a, ApplicationRequest r) {
        a.setCompany(r.company().trim());
        a.setRoleTitle(r.roleTitle().trim());
        a.setJobUrl(r.jobUrl());
        a.setJobDescription(r.jobDescription());
        a.setDeadline(r.deadline());
        a.setNotes(r.notes());
        if (r.status() != null) {
            a.setStatus(r.status());
        }
    }

    private String label(JobApplication a) {
        return a.getCompany() + " - " + a.getRoleTitle();
    }
}
