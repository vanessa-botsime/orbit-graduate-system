# Task board

Planning board for Orbit. To use it on GitHub: create a **Project (Board layout)** with the columns
Backlog, To do, In progress, Done, then turn each unchecked item below into an issue (use the feature
template) and drag it to the right column. Keep this file as the plan of record.

**Rule:** at most 2 items in *In progress*. Finish and deploy before adding the next feature.

## Done

- [x] Repo, README, architecture docs, UML diagrams, ADRs
- [x] Spring Boot API: register/login with BCrypt + JWT, owner-scoped data
- [x] Application CRUD with 8 stages and a "due soon" query
- [x] Kafka event on every change, idempotent audit-log consumer, `/api/audit`
- [x] CV match service (keyword overlap) with unit tests
- [x] Posting risk service (scam-signal rules) with unit tests
- [x] Free-slot finder (planner logic) with unit tests
- [x] React frontend: sign in, pipeline board, due-soon panel, activity log, CV match and risk check
- [x] Dockerfiles, `docker-compose.yml`, GitHub Actions CI

## In progress

- [ ] **First full run:** `mvn verify`, `docker compose up --build`, fix whatever breaks (the scaffold was written without running the full stack)

## To do

### M1: Make the core solid (weeks 1-2)
- [ ] Global error handler returning clear JSON errors
- [ ] Integration tests with Testcontainers (Postgres + Kafka): register, create, update, audit row appears
- [ ] Test that user A cannot read or edit user B's applications
- [ ] Demo account and seed data so reviewers see a populated board
- [ ] Screenshots and a 1-2 minute demo GIF in the README
- [ ] Edit form for notes, job description and link on a card
- [ ] Rate-limit login attempts

### M2: Cloud (weeks 3-4)
- [ ] CI builds and pushes Docker images
- [ ] AWS billing alert **before** creating anything
- [ ] Terraform: network, RDS Postgres, container service, S3 + CloudFront for the frontend
- [ ] Secrets in AWS Secrets Manager (no values in git)
- [ ] Kafka plan for cloud (container on a small instance, not managed MSK)
- [ ] Structured logs, health check, one CloudWatch dashboard
- [ ] Scheduled database backup
- [ ] Live URL in the README

### M3: Data engineering (weeks 5-6)
- [ ] Land raw events as files in S3 (bronze)
- [ ] Clean and structure them (silver)
- [ ] Aggregates: applications per stage, response times, rejection rate (gold)
- [ ] Dashboard over the gold tables
- [ ] Transactional outbox so events are never lost (see ADR-001)
- [ ] `orbit-sentinel` repo: security event monitor consuming the same topic

### M4: Planner and AI review (weeks 7-8)
- [ ] Google OAuth (read-only calendar free/busy)
- [ ] Wire `FreeSlotFinder` to real calendar data
- [ ] Suggest focus blocks before each deadline; create calendar events
- [ ] Optional LLM CV review on top of the keyword match (strip ID number and phone first; document what is sent)
- [ ] Persist match score and risk level in the UI per application (backend already stores them)
- [ ] Risk check lookups: domain age, HTTPS, CIPC search link

### M5: Email detection (stretch)
- [ ] Gmail read-only OAuth; store only sender, subject, date and a short snippet
- [ ] Rules classifier: received, assessment, interview, rejection, "continue your application"
- [ ] Auto-suggest status changes (user confirms)
- [ ] Handle the 7-day refresh-token expiry of apps in Google's testing mode

### M6: Offline use (stretch)
- [ ] PWA manifest and service worker (installable, app shell cached)
- [ ] Read-only offline view of applications and deadlines (IndexedDB)
- [ ] Offline write queue and sync, last-write-wins with timestamps

## Backlog / ideas
- [ ] Move the token from `localStorage` to an httpOnly cookie with CSRF protection
- [ ] Refresh tokens
- [ ] Accessibility audit (keyboard, screen reader, contrast)
- [ ] Metrics page: applications sent, response rate, interviews
- [ ] Export applications to CSV

## Definition of done
A task is done when: code is merged through a pull request, tests cover the new behaviour, CI is green,
the README or docs are updated, and no secrets are in the diff.

## Risks
| Risk | Mitigation |
|---|---|
| Scope creep | Features are frozen per milestone; stretch items can move to "Roadmap" in the README |
| Cloud costs | Billing alert first; tear down unused resources; avoid managed Kafka |
| Applying less because of building | Fixed evening blocks for the project; applications stay a weekly target |
| Claims I cannot defend in interviews | Only list what I can explain line by line |
