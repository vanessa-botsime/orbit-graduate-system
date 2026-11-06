package com.orbit.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    List<JobApplication> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<JobApplication> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<JobApplication> findByOwnerIdAndDeadlineBetweenAndStatusInOrderByDeadlineAsc(
            UUID ownerId, LocalDate from, LocalDate to, Collection<ApplicationStatus> statuses);
}
