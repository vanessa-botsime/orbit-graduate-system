# ADR-002: Stateless JWT authentication

**Status:** Accepted

## Decision
Users register and log in with email and password (BCrypt hashed). The API issues a signed JWT (HS256,
60 minute expiry) whose subject is the user id. Every query is scoped by that id, so one user can never
read another user's applications.

## Consequences
- The API stays stateless and is easy to scale and deploy.
- The secret must come from the environment (`JWT_SECRET`, at least 32 characters) and never from git.
- The frontend keeps the token in `localStorage`, which is exposed if an XSS bug exists. Moving to an
  httpOnly cookie with CSRF protection is on the task board.
- No refresh tokens yet: users sign in again after an hour.
