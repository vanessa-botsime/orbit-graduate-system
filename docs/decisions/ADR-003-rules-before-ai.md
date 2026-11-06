# ADR-003: Rules first, AI later, for CV match and posting risk

**Status:** Accepted

## Context
Two features could use an LLM: scoring a CV against a job description, and judging whether a posting looks
like a scam.

## Decision
Version 1 uses transparent rules: keyword overlap for CV match and weighted scam signals for risk. Each
result lists the reasons behind it.

## Consequences
- Results are explainable and testable offline, with no API cost and no CV data leaving the system.
- The feature is called a "CV match" and a "risk check", not an "ATS score" or a "real or fake" verdict,
  because real ATS systems differ and no automated check can prove a company is genuine.
- An LLM review (suggested rewrites, tone, gaps) can be layered on top later. If added, personal details
  (ID number, phone) must be stripped before sending, and the README must say what is sent.
