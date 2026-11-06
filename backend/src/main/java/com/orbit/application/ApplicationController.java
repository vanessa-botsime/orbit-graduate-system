package com.orbit.application;

import com.orbit.config.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationResponse> list(Authentication auth) {
        return service.list(CurrentUser.id(auth));
    }

    @GetMapping("/due-soon")
    public List<ApplicationResponse> dueSoon(Authentication auth, @RequestParam(defaultValue = "14") int days) {
        return service.dueSoon(CurrentUser.id(auth), Math.min(Math.max(days, 1), 90));
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(Authentication auth, @PathVariable UUID id) {
        return service.get(CurrentUser.id(auth), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(Authentication auth, @Valid @RequestBody ApplicationRequest req) {
        return service.create(CurrentUser.id(auth), req);
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(Authentication auth, @PathVariable UUID id,
                                      @Valid @RequestBody ApplicationRequest req) {
        return service.update(CurrentUser.id(auth), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable UUID id) {
        service.delete(CurrentUser.id(auth), id);
    }
}
