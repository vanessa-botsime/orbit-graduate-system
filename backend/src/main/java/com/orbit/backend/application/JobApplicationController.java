package com.orbit.backend.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationRepository repository;

    public JobApplicationController(JobApplicationRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplication create(@Valid @RequestBody ApplicationRequest request) {
        JobApplication app = new JobApplication();
        app.setCompany(request.company());
        app.setRole(request.role());
        if (request.status() != null) {
            app.setStatus(request.status());
        }
        app.setDeadline(request.deadline());
        app.setAppliedOn(request.appliedOn());
        app.setNotes(request.notes());
        return repository.save(app);
    }

    @GetMapping
    public List<JobApplication> list() {
        return repository.findAll();
    }
}
